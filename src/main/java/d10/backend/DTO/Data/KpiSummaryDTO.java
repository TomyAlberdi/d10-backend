package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KpiSummaryDTO {

    /** Revenue of the requested period, on the requested basis. */
    private Double revenue;
    /** Same metric over the immediately preceding period of equal length. */
    private Double revenuePrevious;

    private Double avgTicket;
    private Double avgTicketPrevious;

    private Integer invoiceCount;
    private Integer invoiceCountPrevious;

    /** Total still owed on DEUDA invoices, right now, not for the period. */
    private Double outstandingDebt;
    private Integer outstandingInvoiceCount;

    /** Capital sitting on the shelves, valued at cost. Point in time. */
    private Double stockValueAtCost;

    /** Cash in minus cash out over the period. USD register left out: other currency. */
    private Double netCash;
}
