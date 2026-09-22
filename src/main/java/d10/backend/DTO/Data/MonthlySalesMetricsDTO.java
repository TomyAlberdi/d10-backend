package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One month of ticket and m2 metrics, optionally narrowed to a category or
 * subcategory.
 *
 * The money figures and the m2 figures count different invoices on purpose:
 * m2 only means something for products sold by the square metre, so the m2
 * ticket divides by the sales that carried at least one of them, and the
 * price per m2 divides only the income of those lines.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySalesMetricsDTO {
    private Integer year;
    private Integer month;

    /** Sales that carried at least one line matching the filter. */
    private Integer invoiceCount;
    /**
     * Invoice totals when unfiltered; the subtotals of the matching lines
     * when a category or subcategory is asked for.
     */
    private Double revenue;
    /** revenue / invoiceCount. */
    private Double avgTicket;

    /** Sales that carried at least one m2 line matching the filter. */
    private Integer m2InvoiceCount;
    /** Square metres sold on M2 lines. */
    private Double surfaceM2;
    /** Subtotals of the M2 lines only. */
    private Double m2Revenue;
    /** surfaceM2 / m2InvoiceCount. */
    private Double avgTicketM2;
    /** m2Revenue / surfaceM2. */
    private Double avgPricePerM2;
}
