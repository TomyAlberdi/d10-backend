package d10.backend.DTO.CashRegister;

import d10.backend.Model.CashRegisterTransaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCashRegisterTransactionDTO {

    private Double amount;

    private CashRegisterTransaction.TransactionType type;

    private String description;

}

