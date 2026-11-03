package d10.backend.DTO.Product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Advanced search filters of the product list, bound from the query string.
 * Every field is optional; a null or blank value does not filter. The price
 * range applies to the price by sale unit.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductFilterDTO {

    private String dimensions;

    private String providerName;

    private String category;

    private String subcategory;

    private Double minPrice;

    private Double maxPrice;

}
