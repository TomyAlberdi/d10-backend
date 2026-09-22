package d10.backend.DTO.Product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyBestSellingProductDTO {

    private String productId;
    private String productName;
    private Integer monthlySales;
    private Double totalValue;
}
