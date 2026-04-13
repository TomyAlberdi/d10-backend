package d10.backend.DTO.Product;

import d10.backend.DTO.TimeSpanEnum;
import d10.backend.Model.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TopSellingProductDTO {
    private Product product;
    private Integer salesAmount;
    private Double totalIncome;
    private TimeSpanEnum timespan;
}
