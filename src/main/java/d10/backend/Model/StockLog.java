package d10.backend.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Document based stock movement log. Replaces the per product
 * {@link ProductStock#getRecordList()} history: every stock in/out operation
 * (manual stock update or invoice) is stored here as an independent document.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "stock_logs")
public class StockLog {

    @Id
    private String id;

    private String productName;
    private String productId;
    // Quantity moved, expressed in the sale unit of the product (CAJA, JUEGO, UNIDAD)
    private Integer saleUnitQuantity;
    private Product.SaleType saleUnitType;
    // Equivalent quantity moved, expressed in the measure unit of the product (M2, ML, MM, UNIDAD)
    private Double measureUnitQuantity;
    private Product.MeasureType measureUnitType;
    private StockLogType type;
    private LocalDateTime datetime;
    // Optional free text, e.g. "Venta #000123" or a note written by the user
    private String detail;

    public enum StockLogType {
        IN, OUT
    }

}
