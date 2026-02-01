package d10.backend.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String code;
    private String name;
    private String description;
//    private String quality;
//    private String mainImage;
//    private List<String> images;
//    private Boolean discontinued=false;
    // Secondary Keys
//    private String providerId;
//    private String categoryId;
//    private String subcategoryId;
    // Measure Data
//    private String measureType;
//    private String measures;
//    private Double measurePrice;
//    private String measureCost;
    // Sale Data
//    private String saleUnit;
//    private String saleUnitPrice;
//    private String saleUnitCost;
//    private Double measurePerSaleUnit;
//    private Double profitMargin;
    // Discount Data
//    private Integer discountPercentage;
//    private Double discountedPrice;
//    private Double discountedMeasurePrice;
    // Characteristics
//    private Characteristics characteristics;
}
