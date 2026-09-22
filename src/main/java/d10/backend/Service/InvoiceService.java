package d10.backend.Service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import d10.backend.DTO.CashRegister.CreateCashRegisterTransactionDTO;
import d10.backend.DTO.Invoice.CreateInvoiceDTO;
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

    public Invoice createInvoice(CreateInvoiceDTO createInvoiceDTO) {
        Invoice invoice = InvoiceMapper.toEntity(createInvoiceDTO);
        invoice.setInvoiceNumber(generateNextInvoiceNumber());
        stampCostSnapshot(invoice);
        if (invoice.getStatus() == Invoice.Status.ENTREGADO || invoice.getStockDecreased().equals(true)) {
            for (InvoiceProduct ip : invoice.getProducts()) {
                productService.checkStockSufficient(ip.getId(), ip.getSaleUnitQuantity());
            }
            for (InvoiceProduct ip : invoice.getProducts()) {
                productService.updateStockDecrease(ip.getId(), ip.getSaleUnitQuantity(), invoice.getDate(), saleStockDetail(invoice));
            }
            invoice.setStockDecreased(true);
        }
        applyDebtStatus(invoice);
        applyClientBalanceEffects(invoice);
        invoiceRepository.save(invoice);
        /*         if ((invoice.getStatus() == Invoice.Status.PAGO || invoice.getStatus() == Invoice.Status.ENVIADO || invoice.getStatus() == Invoice.Status.ENTREGADO) && invoice.getPaymentMethod() != null) {
            addPaymentToCashRegister(invoice);
        } */
        return invoice;
    }

    public Invoice updateInvoice(String id, CreateInvoiceDTO createInvoiceDTO) {
        Invoice invoice = findById(id);
        // The incoming lines come from the frontend and carry no cost
        // snapshot, so the stored ones are kept aside before the mapper
        // replaces the whole product list.
        Map<String, Double> storedCosts = costSnapshotsByProduct(invoice);
        boolean restoredStockForCancellation = false;
        if (Boolean.TRUE.equals(invoice.getStockDecreased()) && createInvoiceDTO.getStatus() == Invoice.Status.CANCELADO) {
            if (invoice.getProducts() != null) {
                for (InvoiceProduct ip : invoice.getProducts()) {
                    int qty = ip.getSaleUnitQuantity() != null ? ip.getSaleUnitQuantity() : 0;
                    if (qty <= 0) {
                        continue;
                    }
                    productService.updateStockIncrease(
                            ip.getId(),
                            qty,
                            invoice.getDate() != null ? invoice.getDate() : LocalDate.now(),
                            cancelledSaleStockDetail(invoice));
                }
            }
            invoice.setStockDecreased(false);
            restoredStockForCancellation = true;
        }
        boolean shouldUpdateStock = !invoice.getStockDecreased() && ((createInvoiceDTO.getStockDecreased().equals(true)) || (createInvoiceDTO.getStatus() == Invoice.Status.ENTREGADO));
        if (shouldUpdateStock) {
            for (InvoiceProduct ip : createInvoiceDTO.getProducts()) {
                productService.checkStockSufficient(ip.getId(), ip.getSaleUnitQuantity());
            }
            for (InvoiceProduct ip : createInvoiceDTO.getProducts()) {
                productService.updateStockDecrease(ip.getId(), ip.getSaleUnitQuantity(), invoice.getDate(), saleStockDetail(invoice));
            }
            invoice.setStockDecreased(true);
        }
        /*         Invoice.Status newStatus = createInvoiceDTO.getStatus();
        boolean isSetToPaid = newStatus == Invoice.Status.PAGO || newStatus == Invoice.Status.ENVIADO || newStatus == Invoice.Status.ENTREGADO;
        boolean isAlreayPaid = invoice.getStatus() == Invoice.Status.PAGO || invoice.getStatus() == Invoice.Status.ENVIADO || invoice.getStatus() == Invoice.Status.ENTREGADO;
        if ((isSetToPaid && !isAlreayPaid) && invoice.getPaymentMethod() != null) {
            addPaymentToCashRegister(invoice);
        } */
        InvoiceMapper.updateFromDTO(invoice, createInvoiceDTO);
        restoreCostSnapshots(invoice, storedCosts);
        stampCostSnapshot(invoice);
        if (restoredStockForCancellation) {
            invoice.setStockDecreased(false);
        }
        applyDebtStatus(invoice);
        invoiceRepository.save(invoice);
        return invoice;
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

    public Invoice updateInvoiceStatus(String id, Invoice.Status newStatus) {
        Invoice invoice = findById(id);
        boolean shouldUpdateStock = !invoice.getStockDecreased()
                && (invoice.getStatus() == Invoice.Status.PENDIENTE || invoice.getStatus() == Invoice.Status.CANCELADO)
                && (newStatus == Invoice.Status.ENTREGADO);
        if (shouldUpdateStock) {
            for (InvoiceProduct ip : invoice.getProducts()) {
                productService.checkStockSufficient(ip.getId(), ip.getSaleUnitQuantity());
            }
            for (InvoiceProduct ip : invoice.getProducts()) {
                productService.updateStockDecrease(ip.getId(), ip.getSaleUnitQuantity(), invoice.getDate(), saleStockDetail(invoice));
            }
            invoice.setStockDecreased(true);
        }
        invoice.setStatus(newStatus);
        applyDebtStatus(invoice);
        invoiceRepository.save(invoice);
        /*         boolean isSetToPaid = newStatus == Invoice.Status.PAGO || newStatus == Invoice.Status.ENVIADO || newStatus == Invoice.Status.ENTREGADO;
        boolean isAlreayPaid = invoice.getStatus() == Invoice.Status.PAGO || invoice.getStatus() == Invoice.Status.ENVIADO || invoice.getStatus() == Invoice.Status.ENTREGADO;
        if ((isSetToPaid && !isAlreayPaid) && invoice.getPaymentMethod() != null) {
            addPaymentToCashRegister(invoice);
        } */
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
     * Keeps a client's balance in sync with a newly created invoice: a debt
     * subtracts the amount still owed, and a fully paid sale consumes the
     * credit balance that was discounted from its total. Nothing happens for
     * statuses that are neither settled nor a debt (e.g. a pending budget),
     * since the sale has not affected the client's money yet.
     */
    private void applyClientBalanceEffects(Invoice invoice) {
        if (invoice.getClient() == null || invoice.getClient().getId() == null) {
            return;
        }
        Client client = clientService.findById(invoice.getClient().getId());
        double clientBalance = client.getBalance() != null ? client.getBalance() : 0.0;
        if (SETTLED_STATUSES.contains(invoice.getStatus())) {
            double requestedDiscount = invoice.getBalanceApplied() != null ? invoice.getBalanceApplied() : 0.0;
            double consumed = Math.max(0, Math.min(requestedDiscount, Math.max(0, clientBalance)));
            if (consumed > 0) {
                clientService.adjustBalance(client.getId(), -consumed);
            }
        } else if (invoice.getStatus() == Invoice.Status.DEUDA) {
            double total = invoice.getTotal() != null ? invoice.getTotal() : 0.0;
            if (total > 0) {
                clientService.adjustBalance(client.getId(), -total);
            }
        }
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
     * Detail stored in the stock log when a cancelled invoice gives its products back.
     */
    private String cancelledSaleStockDetail(Invoice invoice) {
        return invoice.getInvoiceNumber() != null ? "Cancelación venta #" + invoice.getInvoiceNumber() : "Cancelación de venta";
    }

    private void addPaymentToCashRegister(Invoice invoice) {
        if (invoice.getPaymentMethod() == null) {
            return;
        }
        // Exhaustive switch: a new payment method fails to compile until it is
        // mapped to the register it feeds.
        CashRegister.CashRegisterType registerType = switch (invoice.getPaymentMethod()) {
            case CASH -> CashRegister.CashRegisterType.PAPER;
            case DIGITAL -> CashRegister.CashRegisterType.DIGITAL;
            case USD -> CashRegister.CashRegisterType.USD;
        };
        CreateCashRegisterTransactionDTO dto = new CreateCashRegisterTransactionDTO();
        dto.setAmount(invoice.getTotal());
        dto.setType(CashRegisterTransaction.TransactionType.IN);
        dto.setDescription("Pago de venta " + invoice.getInvoiceNumber());
        dto.setRegisterType(registerType);
        cashRegisterService.createTransaction(dto);
    }

}
