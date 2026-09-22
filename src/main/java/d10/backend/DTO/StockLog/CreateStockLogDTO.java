package d10.backend.DTO.StockLog;

import java.time.LocalDateTime;

import d10.backend.Model.StockLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload to register a stock movement manually. The product data
 * (name, sale unit, measure unit and measure equivalent) is resolved
 * server side from the referenced product.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockLogDTO {

    private String productId;

    private Integer saleUnitQuantity;

    private StockLog.StockLogType type;

    private String detail;

    // Optional, defaults to the current date and time
    private LocalDateTime datetime;

}
