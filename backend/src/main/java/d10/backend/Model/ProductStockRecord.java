package d10.backend.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductStockRecord {

    private RecordType type;
    private Integer quantity;
    private LocalDate date;

    public enum RecordType {
        IN, OUT
    }

}
