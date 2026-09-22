package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The monthly ticket and m2 metrics of a whole year, year to date when it is
 * the current one. Ratios are recomputed from the summed figures, never
 * averaged month over month, so a quiet month weighs what it sold.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalesMetricsSummaryDTO {
    private Integer year;
    private Integer invoiceCount;
    private Double revenue;
    private Double avgTicket;
    private Integer m2InvoiceCount;
    private Double surfaceM2;
    private Double m2Revenue;
    private Double avgTicketM2;
    private Double avgPricePerM2;
}
