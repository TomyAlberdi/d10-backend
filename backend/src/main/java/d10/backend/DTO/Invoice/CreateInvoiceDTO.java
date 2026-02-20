package d10.backend.DTO.Invoice;

import d10.backend.Model.Client;
import d10.backend.Model.Invoice;
import d10.backend.Model.InvoiceProduct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateInvoiceDTO {
    private Client client;
    private List<InvoiceProduct> products;
    private Invoice.Status status;
    private Double discount;
    private Double total;
    private Double partialPayment;
    private Invoice.PaymentMethod paymentMethod;
}
