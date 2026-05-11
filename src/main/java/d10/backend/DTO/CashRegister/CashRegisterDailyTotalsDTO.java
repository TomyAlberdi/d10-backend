package d10.backend.DTO.CashRegister;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashRegisterDailyTotalsDTO {

    /** Total amount of transactions of type IN (positive values). */
    private Double inTotal;

    /** Total amount of transactions of type OUT (positive values). */
    private Double outTotal;

}
