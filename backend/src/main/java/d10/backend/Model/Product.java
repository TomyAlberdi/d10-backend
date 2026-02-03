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
    // Measure Data

    // Sale Data
    // Discount Data

    public enum Quality {
        PRIMERA, SEGUNDA
    }

}
