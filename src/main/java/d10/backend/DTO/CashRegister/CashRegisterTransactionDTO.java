package d10.backend.DTO.CashRegister;

import java.time.LocalDateTime;

import d10.backend.Model.CashRegister;
import d10.backend.Model.CashRegisterTransaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashRegisterTransactionDTO {

    private String id;

    private Double amount;

    private CashRegisterTransaction.TransactionType type;

    private String description;

    private LocalDateTime dateTime;

    private CashRegister.CashRegisterType registerType;

}

