package d10.backend.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

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
    private Boolean stockDecreased = false;

    public enum Status {
        PENDIENTE, PAGO, ENVIADO, ENTREGADO, CANCELADO
    }

}
