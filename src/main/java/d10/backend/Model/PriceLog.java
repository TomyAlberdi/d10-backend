package d10.backend.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One change in the pricing of a product. Both sides of the change are kept,
 * so the first entry of a product that already existed before the history was
 * recorded still shows what it used to cost.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "price_logs")
public class PriceLog {

    @Id
    private String id;

    private String productId;
    private String productName;
    private Product.MeasureType measureType;
    private Product.SaleType saleUnitType;
    private LocalDateTime datetime;
    private PriceLogSource source;
    // Optional free text, e.g. the provider and percentage of a bulk update
    private String detail;
    // Null for the entry written when the product is created
    private PriceSnapshot previous;
    private PriceSnapshot current;

    public enum PriceLogSource {
        /** The product was created. */
        CREATED,
        /** The product was edited by hand. */
        EDITED,
        /** A percentage applied to every product of a provider. */
        PROVIDER_UPDATE
    }

    /** The price fields of a product at one point in time. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PriceSnapshot {
        private Double costByMeasureUnit;
        /** Profit percentage over the cost. */
        private Double profit;
        private Double priceByMeasureUnit;
        private Double priceBySaleUnit;

        public static PriceSnapshot of(Product product) {
            return new PriceSnapshot(product.getCostByMeasureUnit(), product.getProfit(),
                    product.getPriceByMeasureUnit(), product.getPriceBySaleUnit());
        }
    }

}
