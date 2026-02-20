package d10.backend.DTO.CashRegister;

import d10.backend.Model.CashRegister;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashRegisterDTO {

    private Double currentAmount;

    private CashRegister.CashRegisterType type;

}

