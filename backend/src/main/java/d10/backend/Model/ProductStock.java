package d10.backend.Model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductStock {

    private Integer quantity;
    private Double measureUnitEquivalent;
    @JsonIgnore
    private List<ProductStockRecord> recordList;

}
