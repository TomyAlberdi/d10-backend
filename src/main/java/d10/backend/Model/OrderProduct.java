package d10.backend.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line of a restock order. The product data is copied when the order is
 * saved so the list keeps reading correctly even if the product is renamed or
 * deleted afterwards.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderProduct {

    // Called productId and not id on purpose: a nested field named id is stored
    // as _id, which breaks every raw query written against this array.
    private String productId;
    private String productCode;
    private String productName;
    private String providerName;
    private Product.SaleType saleUnitType;
    /** Amount to order, expressed in the sale unit of the product. */
    private Integer saleUnitQuantity;
    /** Optional free text, e.g. "Pedir en color blanco". */
    private String detail;

}
