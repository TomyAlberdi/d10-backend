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
}
