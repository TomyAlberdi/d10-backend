package d10.backend.Service;

import d10.backend.DTO.Invoice.CreateInvoiceDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.InvoiceMapper;
import d10.backend.Model.Invoice;
import d10.backend.Repository.InvoiceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public Invoice findById(String id) {
        Optional<Invoice> invoiceSearch = invoiceRepository.findById(id);
        if (invoiceSearch.isEmpty()) {
            throw new ResourceNotFoundException("Presupuesto con ID " +  id + " no encontrado.");
        }
        Invoice invoice = invoiceSearch.get();
        return invoice;
    }

    public Invoice createInvoice(CreateInvoiceDTO createInvoiceDTO) {
        Invoice invoice = InvoiceMapper.toEntity(createInvoiceDTO);
        invoiceRepository.save(invoice);
        return invoice;
    }

    public Invoice updateInvoice(String id, CreateInvoiceDTO createInvoiceDTO) {
        Invoice invoice = findById(id);
        InvoiceMapper.updateFromDTO(invoice, createInvoiceDTO);
        invoiceRepository.save(invoice);
        return invoice;
    }

    public void deleteInvoice(String id) {
        findById(id);
        invoiceRepository.deleteById(id);
    }

}
