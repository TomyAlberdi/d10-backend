package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogQualityDTO {
    private Integer total;
    private Integer active;
    private Integer discontinued;
    private Integer withStock;
    /** Cost missing or zero: these poison every margin and valuation chart. */
    private Integer missingCost;
    private Integer missingPrice;
    private Integer missingCategory;
    private Integer missingSubcategory;
    private Integer missingImages;
    private Integer missingCharacteristics;
    private Integer missingDimensions;
    private Integer missingProvider;
}
