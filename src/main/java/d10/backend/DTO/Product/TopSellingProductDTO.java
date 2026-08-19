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

    /** Number of separate invoices that included this product. */
    private Integer invoiceCount;

    /** Sale units (CAJA / JUEGO / UNIDAD) sold. */
    private Integer unitsSold;

    private Double totalIncome;

    private Double netIncome;

    private Boolean costBasisEstimated;

    private TimeSpanEnum timespan;
}
