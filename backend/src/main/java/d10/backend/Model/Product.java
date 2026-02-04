package d10.backend.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    // Main Data
    private String code;
    private String name;
    private Boolean discontinued = false;
    private ProductStock stock;
    // Secondary Data
    private String description;
    private Quality quality;
    private String providerName;
    private List<ProductCharacteristic> characteristics;
    private List<String> images;
    private String category;
    private String subcategory;
    private String dimensions;
    // Measure Data
    private MeasureType measureType;
    private Double priceByMeasureUnit;
    //private Double costByMeasure;
    // Sale Data
    private SaleType saleUnitType;
    //private Double costBySaleUnit;
    private Double priceBySaleUnit;
    private Double measurePerSaleUnit;
    //private Double profitMargin;
    // Discount Data
    /*
    private Integer discount;
    private Double discountedPriceBySaleUnit;
    private Double discountedPriceByMeasureUnit;
     */

    public enum Quality {
        PRIMERA, SEGUNDA
    }

    public enum MeasureType {
        M2, ML, MM, UNIDAD
    }

    public enum SaleType {
        CAJA, JUEGO, UNIDAD
    }

}
