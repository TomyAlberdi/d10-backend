package d10.backend.DTO.Invoice;

import d10.backend.Model.Invoice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartialInvoiceDTO {
    private String id;
    private String clientName;
    private LocalDate date;
    private Invoice.Status status;
    private Double total;
}
