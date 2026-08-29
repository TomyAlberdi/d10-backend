package d10.backend.DTO.Invoice;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One month of the income chart.
 *
 * {@code income} is the whole of the money collected in the month and is what
 * the chart totals; the two fields under it say where it came from, so a month
 * that looks unusually good can be read as either sales settled on the spot or
 * clients paying down what they already owed.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonthlySummaryRecordDTO {
    private Integer month;
    private Integer year;
    /** settledIncome + debtPayments. */
    private BigDecimal income;
    /** Invoices whose payment covered the total. */
    private BigDecimal settledIncome;
    /** Money already handed over on invoices that are still a debt. */
    private BigDecimal debtPayments;
}
