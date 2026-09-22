package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRevenueDTO {
    /** Category or subcategory name; a placeholder for products with none. */
    private String key;
    private Double revenue;
    private Integer unitsSold;
    /** Distinct products of this group that sold in the period. */
    private Integer skuCount;
    private Double sharePct;
}
