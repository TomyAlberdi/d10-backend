package d10.backend.DTO.Order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line of the payload. Only the product and the amount travel: the code,
 * name, provider and sale unit are resolved server side from the product.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderProductDTO {

    private String productId;

    private Integer saleUnitQuantity;

    private String detail;

}
