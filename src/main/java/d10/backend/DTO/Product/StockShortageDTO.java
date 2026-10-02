package d10.backend.DTO.Product;

import d10.backend.Model.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A product a stock movement would leave below zero, as reported back to the
 * frontend so the user can confirm the movement anyway.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockShortageDTO {
    private String productId;
    private String productName;
    private Product.SaleType saleUnitType;
    private Integer available;
    private Integer required;
    /** available - required, always below zero. */
    private Integer resulting;
}
