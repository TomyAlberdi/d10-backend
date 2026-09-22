package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySalesRecordDTO {
    private Integer year;
    private Integer month;
    /** Everything collected in the month, debtPayments included. */
    private Double income;
    /** The part of {@code income} that came from invoices still owing. */
    private Double debtPayments;
    private Integer invoiceCount;
    private Integer unitsSold;
    private Double surfaceSold;
    private Double avgTicket;
}
