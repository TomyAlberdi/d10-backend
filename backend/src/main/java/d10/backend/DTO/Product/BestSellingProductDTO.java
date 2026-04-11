package d10.backend.DTO.Product;

import d10.backend.Model.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BestSellingProductDTO {
    private Product product;
    private Integer salesAmount;
    private Double totalSurface;
    private Double totalIncome;
    private Double netIncome;
}
