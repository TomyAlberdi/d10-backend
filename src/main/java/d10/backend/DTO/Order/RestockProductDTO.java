package d10.backend.DTO.Order;

import d10.backend.Model.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An active product with no stock left (zero, negative or missing), offered to
 * be added to an order.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestockProductDTO {

    private String productId;
    private String productCode;
    private String productName;
    private String providerName;
    private Product.SaleType saleUnitType;
    /** Current stock in sale units; 0 when the product has no stock data. */
    private Integer stockQuantity;
    /** Sale units already asked for in pending orders, so nothing is ordered twice. */
    private Integer pendingQuantity;

}
