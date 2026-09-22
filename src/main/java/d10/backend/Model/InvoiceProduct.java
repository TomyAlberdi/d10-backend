package d10.backend.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceProduct {
    private String id;
    private String name;
    private Product.MeasureType measureType;
    private Double priceByMeasureUnit;
    private Double measureUnitQuantity;
    private Product.SaleType saleUnitType;
    private Double priceBySaleUnit;
    private Integer saleUnitQuantity;
    private Integer individualDiscount;
    private Double subtotal;
    private String dimensions;
    /**
     * Cost of one measure unit on the day of the sale, copied from the product
     * when the invoice is created.
     *
     * The product document only ever holds the current cost, and
     * {@code ProductService.updateCostsByProvider} rewrites it in bulk every
     * time a supplier raises prices. Without this snapshot a sale from months
     * ago is re-margined against today's cost, so reported profit shrinks with
     * every price update. Null on invoices created before this field existed;
     * the reports fall back to the current product cost for those and flag the
     * result as estimated.
     */
    private Double costByMeasureUnitAtSale;
}
