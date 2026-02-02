package d10.backend.Mapper;

import d10.backend.DTO.Invoice.CreateInvoiceDTO;
import d10.backend.Model.Invoice;

import java.time.LocalDate;

public class InvoiceMapper {

    public static Invoice toEntity(CreateInvoiceDTO createInvoiceDTO) {
        Invoice invoice = new Invoice();
        invoice.setDate(LocalDate.now());
        updateFromDTO(invoice, createInvoiceDTO);
        return invoice;
    }

    public static void updateFromDTO(Invoice invoice, CreateInvoiceDTO createInvoiceDTO) {
        invoice.setClient(createInvoiceDTO.getClient());
        invoice.setProducts(createInvoiceDTO.getProducts());
        invoice.setStatus(createInvoiceDTO.getStatus());
        invoice.setDiscount(createInvoiceDTO.getDiscount());
        invoice.setTotal(createInvoiceDTO.getTotal());
    }

}
