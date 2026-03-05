package d10.backend.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CellItem {
    /** product id stored in this cell */
    private String productId;
    /** quantity of sale units for that product */
    private Integer quantity;
}
