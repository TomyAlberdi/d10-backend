package d10.backend.DTO.Product;

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
    private String code;
    private String name;
    private String description;
    private List<ProductCharacteristic> characteristics;
}
