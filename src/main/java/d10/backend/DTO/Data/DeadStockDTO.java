package d10.backend.DTO.Data;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeadStockDTO {
    /** Capital immobilised by the products below. */
    private Double totalCostValue;
    private Integer itemCount;
    private List<DeadStockItemDTO> items;
}
