package d10.backend.DTO.Product;

import java.time.LocalDate;

import d10.backend.Model.ProductStockRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload of a manual stock update. Besides the movement itself it carries the
 * optional detail written by the user, which is stored in the stock log.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductStockDTO {

    private ProductStockRecord.RecordType type;

    private Integer quantity;

    // Optional, defaults to the current date
    private LocalDate date;

    private String detail;

}
