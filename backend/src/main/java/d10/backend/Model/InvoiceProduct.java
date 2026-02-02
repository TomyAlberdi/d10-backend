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
    private Double measurePrice;
    private Double measureUnitQuantity;
    private Integer saleUnitQuantity;
    private Integer individualDiscount;
    private Double subtotal;
    private String saleUnit;
    private String measureUnit;
    private Double saleUnitPrice;
}
