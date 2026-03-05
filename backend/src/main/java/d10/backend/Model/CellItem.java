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
    /** product stored in this cell */
    private Product product;
    /** quantity of sale units for that product */
    private Integer quantity;
}
