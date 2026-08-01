package d10.backend.DTO.StockLog;

import java.time.LocalDateTime;

import d10.backend.Model.Product;
import d10.backend.Model.StockLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockLogDTO {

    private String id;

    private String productName;

    private String productId;

    private Integer saleUnitQuantity;

    private Product.SaleType saleUnitType;

    private Double measureUnitQuantity;

    private Product.MeasureType measureUnitType;

    private StockLog.StockLogType type;

    private LocalDateTime datetime;

    private String detail;

}
