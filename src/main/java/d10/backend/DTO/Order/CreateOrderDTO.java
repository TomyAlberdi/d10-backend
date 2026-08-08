package d10.backend.DTO.Order;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload to create or update a restock order. The product data (code, name,
 * provider and sale unit) is resolved server side from the referenced product.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDTO {

    private String productId;

    private Integer saleUnitQuantity;

    // Optional, defaults to the current date
    private LocalDate orderDate;

    private String detail;

}
