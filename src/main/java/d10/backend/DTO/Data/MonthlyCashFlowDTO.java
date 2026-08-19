package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cash movements of one month, read from the cash register transaction ledger.
 * Field names match {@code CashRegisterDailyTotalsDTO} so both read the same
 * way on the frontend.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyCashFlowDTO {
    private Integer month;
    private Integer year;
    private Double inTotal;
    private Double outTotal;
    private Double net;
}
