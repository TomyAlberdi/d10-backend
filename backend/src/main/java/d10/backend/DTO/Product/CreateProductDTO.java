package d10.backend.DTO.Product;

import d10.backend.Model.Product;
import d10.backend.Model.ProductCharacteristic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductDTO {
    // Main Data
    private String code;
    private String name;
    // Secondary Data
    private String description;
    private Product.Quality quality;
    private String providerName;
    private List<ProductCharacteristic> characteristics;
    private List<String> images;
    private String category;
    private String subcategory;
    private String dimensions;
    // Measure Data
    private Product.MeasureType measureType;
    // Sale Data
    private Product.SaleType saleUnitType;
    private Double priceBySaleUnit;
    private Double measurePerSaleUnit;
}
