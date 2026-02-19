package d10.backend.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import d10.backend.DTO.CashRegister.CreateCashRegisterTransactionDTO;
import d10.backend.DTO.Invoice.CreateInvoiceDTO;
import d10.backend.Exception.ExistingAttributeException;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Model.Client;
import d10.backend.Mapper.InvoiceMapper;
import d10.backend.Model.CashRegisterTransaction;
import d10.backend.Model.Invoice;
import d10.backend.Model.InvoiceProduct;
import d10.backend.Repository.InvoiceRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProductService productService;
    private final CashRegisterService cashRegisterService;

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
        double registeredPayment = sanitizePayment(createInvoiceDTO.getPartialPayment());
        validatePayment(registeredPayment, invoice.getTotal());
        invoice.setPaidAmount(registeredPayment);
        registerPartialPayment(createInvoiceDTO.getClient(), registeredPayment, createInvoiceDTO.getStatus());
        if (invoice.getStatus() == Invoice.Status.PAGO || invoice.getStatus() == Invoice.Status.ENVIADO || invoice.getStatus() == Invoice.Status.ENTREGADO) {
            for (InvoiceProduct ip : invoice.getProducts()) {
                productService.checkStockSufficient(ip.getId(), ip.getSaleUnitQuantity());
            }
            for (InvoiceProduct ip : invoice.getProducts()) {
                productService.updateStockDecrease(ip.getId(), ip.getSaleUnitQuantity(), invoice.getDate());
            }
            invoice.setStockDecreased(true);
        }
        invoiceRepository.save(invoice);
        return invoice;
    }

    public Invoice updateInvoice(String id, CreateInvoiceDTO createInvoiceDTO) {
        Invoice invoice = findById(id);
        double currentPaidAmount = sanitizePayment(invoice.getPaidAmount());
        double registeredPayment = sanitizePayment(createInvoiceDTO.getPartialPayment());
        double updatedPaidAmount = currentPaidAmount + registeredPayment;
        validatePayment(updatedPaidAmount, createInvoiceDTO.getTotal());
        registerPartialPayment(createInvoiceDTO.getClient(), registeredPayment, createInvoiceDTO.getStatus());
        boolean shouldUpdateStock = !invoice.getStockDecreased() &&
                (invoice.getStatus() == Invoice.Status.PENDIENTE || invoice.getStatus() == Invoice.Status.CANCELADO) &&
                (createInvoiceDTO.getStatus() == Invoice.Status.PAGO || createInvoiceDTO.getStatus() == Invoice.Status.ENVIADO || createInvoiceDTO.getStatus() == Invoice.Status.ENTREGADO);
        if (shouldUpdateStock) {
            for (InvoiceProduct ip : createInvoiceDTO.getProducts()) {
                productService.checkStockSufficient(ip.getId(), ip.getSaleUnitQuantity());
            }
            for (InvoiceProduct ip : createInvoiceDTO.getProducts()) {
                productService.updateStockDecrease(ip.getId(), ip.getSaleUnitQuantity(), invoice.getDate());
            }
            invoice.setStockDecreased(true);
        }
        InvoiceMapper.updateFromDTO(invoice, createInvoiceDTO);
        invoice.setPaidAmount(updatedPaidAmount);
        invoiceRepository.save(invoice);
        return invoice;
    }

    private double sanitizePayment(Double payment) {
        return payment == null ? 0.0 : payment;
    }

    private void validatePayment(double paidAmount, Double total) {
        if (paidAmount < 0) {
            throw new ExistingAttributeException("El pago parcial no puede ser negativo.");
        }
        if (total == null || total < 0) {
            throw new ExistingAttributeException("El total de la factura es inválido.");
        }
        if (paidAmount > total) {
            throw new ExistingAttributeException("El pago registrado no puede superar el total de la factura.");
        }
    }

    private void registerPartialPayment(Client client, double paymentAmount, Invoice.Status status) {
        if (paymentAmount <= 0) {
            return;
        }
        String clientName = (client != null && client.getName() != null) ? client.getName() : "SIN CLIENTE";
        String description = status == Invoice.Status.PAGO
                ? "PAGO TOTAL - " + clientName
                : "PAGO PARCIAL /" + clientName + "/";
        CreateCashRegisterTransactionDTO transaction = new CreateCashRegisterTransactionDTO(
                paymentAmount,
                CashRegisterTransaction.TransactionType.IN,
                description
        );
        cashRegisterService.createTransaction(transaction);
    }

    public void deleteInvoice(String id) {
        findById(id);
        invoiceRepository.deleteById(id);
    }

    public List<Invoice> searchInvoices(String q) {
        if (q == null) {
            return invoiceRepository.findTop15ByOrderByDateDesc();
        }
        return invoiceRepository.findByClientCuitDniContainingIgnoreCaseOrClientNameContainingIgnoreCase(q, q);
    }

    public Invoice updateInvoiceStatus(String id, Invoice.Status newStatus) {
        Invoice invoice = findById(id);
        validatePayment(sanitizePayment(invoice.getPaidAmount()), invoice.getTotal());
        boolean shouldUpdateStock = !invoice.getStockDecreased() &&
                (invoice.getStatus() == Invoice.Status.PENDIENTE || invoice.getStatus() == Invoice.Status.CANCELADO) &&
                (newStatus == Invoice.Status.PAGO || newStatus == Invoice.Status.ENVIADO || newStatus == Invoice.Status.ENTREGADO);
        if (shouldUpdateStock) {
            for (InvoiceProduct ip : invoice.getProducts()) {
                productService.checkStockSufficient(ip.getId(), ip.getSaleUnitQuantity());
            }
            for (InvoiceProduct ip : invoice.getProducts()) {
                productService.updateStockDecrease(ip.getId(), ip.getSaleUnitQuantity(), invoice.getDate());
            }
            invoice.setStockDecreased(true);
        }
        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);
        return invoice;
    }

}
