package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockValuationDTO {
    /** Category, provider or quality, depending on the requested grouping. */
    private String key;
    private Integer skuCount;
    private Integer unitsOnHand;
    private Double costValue;
    private Double retailValue;
    /** What the stock would earn if it all sold at list price. */
    private Double potentialMargin;
    private Double sharePct;
}
