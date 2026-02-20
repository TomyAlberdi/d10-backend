package d10.backend.Model;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "invoices")
public class Invoice {

    @Id
    private String id;

    private LocalDate date;
    private Client client;
    private Status status;
    private List<InvoiceProduct> products;
    private Double discount;
    private Double total;
    private Double partialPayment = 0.0;
    private Boolean stockDecreased = false;
    private String invoiceNumber;

    public enum Status {
        PENDIENTE, PAGO, ENVIADO, ENTREGADO, CANCELADO
    }

}
