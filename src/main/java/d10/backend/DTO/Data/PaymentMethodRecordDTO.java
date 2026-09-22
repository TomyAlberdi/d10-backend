package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodRecordDTO {
    private Integer year;
    private Integer month;
    private Double cash;
    private Double digital;
    private Double usd;
    /**
     * Invoices with no payment method recorded. The field was added after the
     * system was already in use, so historical sales land here rather than
     * being silently attributed to cash.
     */
    private Double unspecified;
    private Double total;
}
