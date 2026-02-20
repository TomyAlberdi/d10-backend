package d10.backend.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cash_register_transactions")
public class CashRegisterTransaction {

    @Id
    private String id;

    private Double amount;

    private TransactionType type;

    private String description;

    private LocalDateTime dateTime;

    private CashRegister.CashRegisterType registerType;

    public enum TransactionType {
        IN, OUT
    }

}

