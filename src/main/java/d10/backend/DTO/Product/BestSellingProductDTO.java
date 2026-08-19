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

    /** Number of separate invoices that included this product. */
    private Integer invoiceCount;

    /** Sale units (CAJA / JUEGO / UNIDAD) sold. */
    private Integer unitsSold;

    /** Measure units (m2 / ml / mm / unidad) sold. */
    private Double totalSurface;

    private Double totalIncome;

    /** Null when no cost is known for the units sold. */
    private Double netIncome;

    /**
     * True when part of the cost had to be taken from the product's current
     * cost because those invoice lines carry no cost snapshot. The margin is
     * then an approximation, not a historical fact.
     */
    private Boolean costBasisEstimated;
}
