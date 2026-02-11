package d10.backend.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cash_register")
public class CashRegister {

    public static final String CASH_REGISTER_ID = "CASH_REGISTER";

    @Id
    private String id = CASH_REGISTER_ID;

    private Double currentAmount = 0.0;

}

