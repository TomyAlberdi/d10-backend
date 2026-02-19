package d10.backend.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import d10.backend.DTO.Invoice.CreateInvoiceDTO;
import d10.backend.Exception.ExistingAttributeException;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.InvoiceMapper;
import d10.backend.Model.Invoice;
import d10.backend.Model.InvoiceProduct;
import d10.backend.Repository.InvoiceRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProductService productService;

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
        boolean shouldUpdateStock = !invoice.getStockDecreased()
                && (invoice.getStatus() == Invoice.Status.PENDIENTE || invoice.getStatus() == Invoice.Status.CANCELADO)
                && (createInvoiceDTO.getStatus() == Invoice.Status.PAGO || createInvoiceDTO.getStatus() == Invoice.Status.ENVIADO || createInvoiceDTO.getStatus() == Invoice.Status.ENTREGADO);
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
        boolean shouldUpdateStock = !invoice.getStockDecreased()
                && (invoice.getStatus() == Invoice.Status.PENDIENTE || invoice.getStatus() == Invoice.Status.CANCELADO)
                && (newStatus == Invoice.Status.PAGO || newStatus == Invoice.Status.ENVIADO || newStatus == Invoice.Status.ENTREGADO);
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
