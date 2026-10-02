package d10.backend.Service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import d10.backend.DTO.Invoice.CreateInvoiceDTO;
import d10.backend.DTO.Invoice.InvoicePaymentDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.InvoiceMapper;
import d10.backend.Model.CashRegister;
import d10.backend.Model.CashRegisterTransaction;
import d10.backend.Model.Client;
import d10.backend.Model.Invoice;
import d10.backend.Model.InvoiceProduct;
import d10.backend.Model.Product;
import d10.backend.Repository.InvoiceRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProductService productService;
    private final CashRegisterService cashRegisterService;
    private final ClientService clientService;

    /**
     * Statuses that mean the sale is settled/paid in full. Mirrors the
     * frontend's SETTLED_STATUSES in lib/invoice.ts.
     */
    private static final Set<Invoice.Status> SETTLED_STATUSES =
            EnumSet.of(Invoice.Status.PAGO, Invoice.Status.ENVIADO, Invoice.Status.ENTREGADO);

    /**
     * Tolerance used when comparing amounts: a balance under a cent is paid.
     */
    private static final double PAYMENT_TOLERANCE = 0.01;

    private String generateNextInvoiceNumber() {
        Optional<Invoice> lastInvoice = invoiceRepository.findTopByOrderByInvoiceNumberDesc();
        int nextNum = 1;
        if (lastInvoice.isPresent() && lastInvoice.get().getInvoiceNumber() != null) {
            try {
                nextNum = Integer.parseInt(lastInvoice.get().getInvoiceNumber()) + 1;
            } catch (NumberFormatException e) {
                // if not a number, start from 1
            }
        }
        return String.format("%06d", nextNum);
    }

    public Invoice findById(String id) {
        Optional<Invoice> invoiceSearch = invoiceRepository.findById(id);
        if (invoiceSearch.isEmpty()) {
            throw new ResourceNotFoundException("Presupuesto con ID " + id + " no encontrado.");
        }
        Invoice invoice = invoiceSearch.get();
        return invoice;
    }

    public Invoice createInvoice(CreateInvoiceDTO createInvoiceDTO, boolean allowNegativeStock) {
        ResolvedPayment payment = resolvePayment(createInvoiceDTO.getPayment(), createInvoiceDTO.getStatus());

        Invoice invoice = InvoiceMapper.toEntity(createInvoiceDTO);
        invoice.setInvoiceNumber(generateNextInvoiceNumber());
        stampCostSnapshot(invoice);
        boolean takesStock = invoice.getStatus() == Invoice.Status.ENTREGADO
                || Boolean.TRUE.equals(invoice.getStockDecreased());
        invoice.setStockDecreased(takesStock);
        applyPayment(invoice, 0.0, payment);
        applyDebtStatus(invoice);
        checkSettledIsCollected(invoice, 0.0, payment);
        if (takesStock) {
            productService.checkNegativeStock(requiredStock(invoice.getProducts()), allowNegativeStock);
        }

        // Every check passed: write.
        if (takesStock) {
            for (InvoiceProduct ip : invoice.getProducts()) {
                productService.updateStockDecrease(ip.getId(), ip.getSaleUnitQuantity(), invoice.getDate(), saleStockDetail(invoice));
            }
        }
        invoiceRepository.save(invoice);
        registerPayment(invoice, payment);
        applySettledCredit(invoice);
        syncClientDebt(null, 0.0, invoice);
        return invoice;
    }

    public Invoice updateInvoice(String id, CreateInvoiceDTO createInvoiceDTO, boolean allowNegativeStock) {
        Invoice invoice = findById(id);
        checkClientUnchangedOnDebt(invoice, createInvoiceDTO);
        ResolvedPayment payment = resolvePayment(createInvoiceDTO.getPayment(), createInvoiceDTO.getStatus());

        // The sale as it was, for the stock and balance differences below.
        String clientIdBefore = clientIdOf(invoice);
        double owedBefore = owed(invoice);
        double paidBefore = invoice.getPartialPayment() != null ? invoice.getPartialPayment() : 0.0;
        boolean stockTakenBefore = Boolean.TRUE.equals(invoice.getStockDecreased());
        Map<String, Integer> stockBefore = requiredStock(invoice.getProducts());
        Map<String, Integer> stockAfter = requiredStock(createInvoiceDTO.getProducts());
        boolean cancelling = createInvoiceDTO.getStatus() == Invoice.Status.CANCELADO;
        boolean takesStockNow = !stockTakenBefore && !cancelling
                && (Boolean.TRUE.equals(createInvoiceDTO.getStockDecreased())
                        || createInvoiceDTO.getStatus() == Invoice.Status.ENTREGADO);
        Map<CashRegister.CashRegisterType, Double> refunds = cancelling && Boolean.TRUE.equals(createInvoiceDTO.getRefundPayments())
                ? cashRegisterService.netCollectedByRegister(invoice.getId())
                : Map.of();

        // The incoming lines come from the frontend and carry no cost
        // snapshot, so the stored ones are kept aside before the mapper
        // replaces the whole product list.
        Map<String, Double> storedCosts = costSnapshotsByProduct(invoice);
        InvoiceMapper.updateFromDTO(invoice, createInvoiceDTO);
        restoreCostSnapshots(invoice, storedCosts);
        stampCostSnapshot(invoice);
        invoice.setStockDecreased(cancelling ? false : stockTakenBefore || takesStockNow);
        applyPayment(invoice, paidBefore, payment);
        applyDebtStatus(invoice);
        checkSettledIsCollected(invoice, owedBefore, payment);

        // Stock: give everything back on cancel, take everything when the sale
        // first takes stock, or move only the difference on an edit.
        Map<String, Integer> toTake = new LinkedHashMap<>();
        Map<String, Integer> toGiveBack = new LinkedHashMap<>();
        if (stockTakenBefore && cancelling) {
            toGiveBack.putAll(stockBefore);
        } else if (takesStockNow) {
            toTake.putAll(stockAfter);
        } else if (stockTakenBefore) {
            stockAfter.forEach((productId, qty) -> {
                int diff = qty - stockBefore.getOrDefault(productId, 0);
                if (diff > 0) {
                    toTake.put(productId, diff);
                }
            });
            stockBefore.forEach((productId, qty) -> {
                int diff = qty - stockAfter.getOrDefault(productId, 0);
                if (diff > 0) {
                    toGiveBack.put(productId, diff);
                }
            });
        }
        productService.checkNegativeStock(toTake, allowNegativeStock);

        // Every check passed: write.
        LocalDate stockDate = invoice.getDate() != null ? invoice.getDate() : LocalDate.now();
        String takeDetail = takesStockNow ? saleStockDetail(invoice) : editedSaleStockDetail(invoice);
        String giveBackDetail = cancelling ? cancelledSaleStockDetail(invoice) : editedSaleStockDetail(invoice);
        toTake.forEach((productId, qty) -> productService.updateStockDecrease(productId, qty, stockDate, takeDetail));
        toGiveBack.forEach((productId, qty) -> productService.updateStockIncrease(productId, qty, stockDate, giveBackDetail));
        invoiceRepository.save(invoice);
        registerPayment(invoice, payment);
        refunds.forEach((registerType, net) -> {
            if (net >= PAYMENT_TOLERANCE) {
                cashRegisterService.createInvoiceTransaction(invoice, net, registerType,
                        CashRegisterTransaction.TransactionType.OUT);
            }
        });
        syncClientDebt(clientIdBefore, owedBefore, invoice);
        return invoice;
    }

    /** A request's payment after validation; amounts are never null. */
    private record ResolvedPayment(double registerAmount, CashRegister.CashRegisterType registerType,
            double countedAmount, boolean partial) {
    }

    /** Statuses a payment can be registered with. */
    private static final Set<Invoice.Status> COLLECTABLE_STATUSES =
            EnumSet.of(Invoice.Status.PAGO, Invoice.Status.ENVIADO, Invoice.Status.ENTREGADO, Invoice.Status.DEUDA);

    /**
     * Checks the payment sent with a sale before anything is written. Returns
     * null when there is none.
     */
    private static ResolvedPayment resolvePayment(InvoicePaymentDTO dto, Invoice.Status status) {
        if (dto == null) {
            return null;
        }
        if (!COLLECTABLE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Solo se puede registrar un cobro en una venta pagada, entregada o con deuda.");
        }
        if (dto.getRegisterType() == null) {
            throw new IllegalArgumentException("Elegí la caja donde entra el cobro.");
        }
        boolean partial = Boolean.TRUE.equals(dto.getPartial());
        double amount = dto.getAmount() != null ? dto.getAmount() : 0.0;
        if (amount < 0 || (!partial && amount <= 0)) {
            throw new IllegalArgumentException("El monto cobrado debe ser mayor a 0.");
        }
        double counted = amount;
        if (dto.getRegisterType() == CashRegister.CashRegisterType.USD && partial) {
            double pesos = dto.getPesoAmount() != null ? dto.getPesoAmount() : -1;
            if (amount > 0 && pesos < 0) {
                throw new IllegalArgumentException("Indicá cuántos pesos cubre el pago en USD.");
            }
            counted = Math.max(0, pesos);
        }
        return new ResolvedPayment(amount, dto.getRegisterType(), counted, partial);
    }

    /**
     * Sets what the sale counts as paid. A full payment settles it, whatever
     * was actually handed over; a partial one adds the amount it covers and
     * leaves the rest as a debt. Without a payment the paid amount is the one
     * stored before, never the one sent by the frontend.
     */
    private static void applyPayment(Invoice invoice, double paidBefore, ResolvedPayment payment) {
        double total = invoice.getTotal() != null ? invoice.getTotal() : 0.0;
        if (payment == null) {
            invoice.setPartialPayment(Math.min(total, paidBefore));
            return;
        }
        invoice.setPaymentMethod(paymentMethodOf(payment.registerType()));
        double paid = payment.partial() ? Math.min(total, paidBefore + payment.countedAmount()) : total;
        invoice.setPartialPayment(paid);
        if (total - paid >= PAYMENT_TOLERANCE) {
            invoice.setStatus(Invoice.Status.DEUDA);
        } else if (invoice.getStatus() == Invoice.Status.DEUDA) {
            // A debt paid off: delivered if its stock already left, paid otherwise.
            invoice.setStatus(Boolean.TRUE.equals(invoice.getStockDecreased())
                    ? Invoice.Status.ENTREGADO
                    : Invoice.Status.PAGO);
        }
    }

    /**
     * A settled sale must have its money registered: one that ends up owing
     * more than it did before, without a payment in the same request, is
     * rejected instead of being saved as paid.
     */
    private static void checkSettledIsCollected(Invoice invoice, double owedBefore, ResolvedPayment payment) {
        if (payment != null || !SETTLED_STATUSES.contains(invoice.getStatus())) {
            return;
        }
        double total = invoice.getTotal() != null ? invoice.getTotal() : 0.0;
        double paid = invoice.getPartialPayment() != null ? invoice.getPartialPayment() : 0.0;
        double unpaid = total - paid;
        if (unpaid >= PAYMENT_TOLERANCE && unpaid - owedBefore >= PAYMENT_TOLERANCE) {
            throw new IllegalArgumentException("Registrá el cobro de la venta para marcarla como pagada.");
        }
    }

    /** Writes the payment's transaction, linked to the sale. */
    private void registerPayment(Invoice invoice, ResolvedPayment payment) {
        if (payment == null || payment.registerAmount() <= 0) {
            return;
        }
        cashRegisterService.createInvoiceTransaction(invoice, payment.registerAmount(), payment.registerType(),
                CashRegisterTransaction.TransactionType.IN);
    }

    private static Invoice.PaymentMethod paymentMethodOf(CashRegister.CashRegisterType registerType) {
        // Exhaustive switch: a new register fails to compile until it is mapped.
        return switch (registerType) {
            case PAPER -> Invoice.PaymentMethod.CASH;
            case DIGITAL -> Invoice.PaymentMethod.DIGITAL;
            case USD -> Invoice.PaymentMethod.USD;
        };
    }

    /**
     * A debt was charged to its client's balance when it was created, and an
     * edit does not move that charge, so the client of a debt stays fixed.
     */
    private static void checkClientUnchangedOnDebt(Invoice invoice, CreateInvoiceDTO dto) {
        if (invoice.getStatus() != Invoice.Status.DEUDA) {
            return;
        }
        String currentClientId = invoice.getClient() != null ? invoice.getClient().getId() : null;
        String requestedClientId = dto.getClient() != null ? dto.getClient().getId() : null;
        if (!Objects.equals(currentClientId, requestedClientId)) {
            throw new IllegalStateException("No se puede cambiar el cliente de una venta con deuda.");
        }
    }

    /** Sale units each product of the invoice takes out, lines of the same product summed. */
    private static Map<String, Integer> requiredStock(List<InvoiceProduct> products) {
        Map<String, Integer> required = new LinkedHashMap<>();
        if (products != null) {
            for (InvoiceProduct ip : products) {
                ProductService.addRequired(required, ip.getId(), ip.getSaleUnitQuantity());
            }
        }
        return required;
    }

    public void deleteInvoice(String id) {
        findById(id);
        invoiceRepository.deleteById(id);
    }

    public List<Invoice> searchInvoices(String q) {
        return searchInvoices(q, null, null, null);
    }

    public List<Invoice> searchInvoices(String q, LocalDate from, LocalDate to) {
        return searchInvoices(q, null, from, to);
    }

    public List<Invoice> searchInvoices(String q, Invoice.Status status) {
        return searchInvoices(q, status, null, null);
    }

    public List<Invoice> searchInvoices(String q, Invoice.Status status, LocalDate from, LocalDate to) {
        List<Invoice> results;
        if (q == null || q.trim().isEmpty()) {
            if (status == null) {
                results = invoiceRepository.findTop25ByOrderByDateDescInvoiceNumberDesc();
            } else {
                results = invoiceRepository.findByStatusOrderByDateDescInvoiceNumberDesc(status);
            }
        } else if (status == null) {
            results = invoiceRepository.findByInvoiceNumberOrClientCuitDniOrClientName(q);
        } else {
            results = invoiceRepository.findByStatusAndInvoiceNumberOrClientCuitDniOrClientName(status, q);
        }

        if (from == null && to == null) {
            return results;
        }

        LocalDate start = from != null ? from : LocalDate.of(1900, 1, 1);
        LocalDate end = to != null ? to : LocalDate.now().plusDays(1);
        return results.stream()
                .filter(invoice -> invoice.getDate() != null)
                .filter(invoice -> !invoice.getDate().isBefore(start))
                .filter(invoice -> !invoice.getDate().isAfter(end))
                .toList();
    }

    public List<Invoice> getInvoicesWithStockNotDecreased() {
        return invoiceRepository.findByStockDecreasedFalseOrderByDateDescInvoiceNumberDesc();
    }

    public List<Invoice> findInvoicesByProductId(String productId) {
        return invoiceRepository.findByProductId(productId);
    }

    public Invoice updateInvoiceStatus(String id, Invoice.Status newStatus, boolean allowNegativeStock) {
        Invoice invoice = findById(id);
        boolean shouldUpdateStock = !invoice.getStockDecreased()
                && (invoice.getStatus() == Invoice.Status.PENDIENTE || invoice.getStatus() == Invoice.Status.CANCELADO)
                && (newStatus == Invoice.Status.ENTREGADO);
        if (shouldUpdateStock) {
            productService.checkNegativeStock(requiredStock(invoice.getProducts()), allowNegativeStock);
            for (InvoiceProduct ip : invoice.getProducts()) {
                productService.updateStockDecrease(ip.getId(), ip.getSaleUnitQuantity(), invoice.getDate(), saleStockDetail(invoice));
            }
            invoice.setStockDecreased(true);
        }
        invoice.setStatus(newStatus);
        applyDebtStatus(invoice);
        invoiceRepository.save(invoice);
        return invoice;
    }

    /**
     * A sale whose products already left the stock and whose payment does not
     * cover the total is a debt, no matter which status was requested.
     * Cancelled sales keep their status: they give their stock back instead of
     * being collected.
     */
    private void applyDebtStatus(Invoice invoice) {
        if (invoice.getStatus() == Invoice.Status.CANCELADO || !Boolean.TRUE.equals(invoice.getStockDecreased())) {
            return;
        }
        double total = invoice.getTotal() != null ? invoice.getTotal() : 0.0;
        double paid = invoice.getPartialPayment() != null ? invoice.getPartialPayment() : 0.0;
        if (total - paid >= PAYMENT_TOLERANCE) {
            invoice.setStatus(Invoice.Status.DEUDA);
        }
    }

    /**
     * A settled sale consumes the credit balance that was discounted from its
     * total, clamped to what the client actually has. Only on creation.
     */
    private void applySettledCredit(Invoice invoice) {
        String clientId = clientIdOf(invoice);
        if (clientId == null || !SETTLED_STATUSES.contains(invoice.getStatus())) {
            return;
        }
        Client client = clientService.findById(clientId);
        double clientBalance = client.getBalance() != null ? client.getBalance() : 0.0;
        double requestedDiscount = invoice.getBalanceApplied() != null ? invoice.getBalanceApplied() : 0.0;
        double consumed = Math.max(0, Math.min(requestedDiscount, Math.max(0, clientBalance)));
        if (consumed > 0) {
            clientService.adjustBalance(clientId, -consumed);
        }
    }

    /** What a sale's client still owes for it: only a debt with a client owes. */
    private static double owed(Invoice invoice) {
        if (invoice.getStatus() != Invoice.Status.DEUDA || clientIdOf(invoice) == null) {
            return 0.0;
        }
        double total = invoice.getTotal() != null ? invoice.getTotal() : 0.0;
        double paid = invoice.getPartialPayment() != null ? invoice.getPartialPayment() : 0.0;
        return Math.max(0, total - paid);
    }

    /**
     * Keeps the client's balance equal to what their debts owe: the amount the
     * sale owed before is given back to whoever owed it, and what it owes now
     * is charged to its client. Covers new debts, payments on a debt, totals
     * that change on a debt and debts that are cancelled or paid off.
     */
    private void syncClientDebt(String clientIdBefore, double owedBefore, Invoice invoice) {
        String clientIdAfter = clientIdOf(invoice);
        double owedAfter = owed(invoice);
        if (Objects.equals(clientIdBefore, clientIdAfter)) {
            double delta = owedAfter - owedBefore;
            if (clientIdAfter != null && Math.abs(delta) >= PAYMENT_TOLERANCE) {
                clientService.adjustBalance(clientIdAfter, -delta);
            }
            return;
        }
        if (clientIdBefore != null && owedBefore >= PAYMENT_TOLERANCE) {
            clientService.adjustBalance(clientIdBefore, owedBefore);
        }
        if (clientIdAfter != null && owedAfter >= PAYMENT_TOLERANCE) {
            clientService.adjustBalance(clientIdAfter, -owedAfter);
        }
    }

    private static String clientIdOf(Invoice invoice) {
        return invoice.getClient() != null ? invoice.getClient().getId() : null;
    }

    /**
     * Copies the cost each product has today onto its invoice line.
     *
     * The product document only ever holds the current cost, and
     * updateCostsByProvider rewrites it in bulk on every supplier increase, so
     * without this a sale is re-margined against a cost that did not exist
     * when it happened. Lines that already carry a snapshot are left alone:
     * overwriting them would rewrite history.
     */
    private void stampCostSnapshot(Invoice invoice) {
        List<InvoiceProduct> lines = invoice.getProducts();
        if (lines == null || lines.isEmpty()) {
            return;
        }
        List<String> pendingIds = lines.stream()
                .filter(line -> line.getCostByMeasureUnitAtSale() == null && line.getId() != null)
                .map(InvoiceProduct::getId)
                .distinct()
                .toList();
        if (pendingIds.isEmpty()) {
            return;
        }
        Map<String, Product> products = productService.findByIds(pendingIds);
        for (InvoiceProduct line : lines) {
            if (line.getCostByMeasureUnitAtSale() != null) {
                continue;
            }
            Product product = products.get(line.getId());
            if (product != null) {
                line.setCostByMeasureUnitAtSale(product.getCostByMeasureUnit());
            }
        }
    }

    /**
     * Cost snapshots of an invoice, keyed by product, so they survive an edit.
     */
    private Map<String, Double> costSnapshotsByProduct(Invoice invoice) {
        Map<String, Double> costs = new HashMap<>();
        if (invoice.getProducts() == null) {
            return costs;
        }
        for (InvoiceProduct line : invoice.getProducts()) {
            if (line.getId() != null && line.getCostByMeasureUnitAtSale() != null) {
                costs.putIfAbsent(line.getId(), line.getCostByMeasureUnitAtSale());
            }
        }
        return costs;
    }

    /**
     * Puts the previously stored snapshots back on the lines that kept selling
     * the same product. Lines added during the edit stay empty and are stamped
     * with the current cost afterwards.
     */
    private void restoreCostSnapshots(Invoice invoice, Map<String, Double> storedCosts) {
        if (invoice.getProducts() == null || storedCosts.isEmpty()) {
            return;
        }
        for (InvoiceProduct line : invoice.getProducts()) {
            if (line.getCostByMeasureUnitAtSale() == null) {
                Double stored = storedCosts.get(line.getId());
                if (stored != null) {
                    line.setCostByMeasureUnitAtSale(stored);
                }
            }
        }
    }

    /**
     * Detail stored in the stock log when an invoice takes products out of stock.
     */
    private String saleStockDetail(Invoice invoice) {
        return invoice.getInvoiceNumber() != null ? "Venta #" + invoice.getInvoiceNumber() : "Venta";
    }

    /**
     * Detail stored in the stock log when an edit changes the products of a
     * sale that had already taken its stock.
     */
    private String editedSaleStockDetail(Invoice invoice) {
        return invoice.getInvoiceNumber() != null ? "Edición venta #" + invoice.getInvoiceNumber() : "Edición de venta";
    }

    /**
     * Detail stored in the stock log when a cancelled invoice gives its products back.
     */
    private String cancelledSaleStockDetail(Invoice invoice) {
        return invoice.getInvoiceNumber() != null ? "Cancelación venta #" + invoice.getInvoiceNumber() : "Cancelación de venta";
    }

}
