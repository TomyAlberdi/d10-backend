package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DebtorDTO {
    private String clientId;
    private String clientName;
    /** Total invoiced minus what has already been paid. */
    private Double outstanding;
    private Double totalInvoiced;
    private Double paidPct;
    private Integer invoiceCount;
    private Integer oldestInvoiceDays;
}
