package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockTurnoverDTO {
    private String productId;
    private String code;
    private String name;
    private String category;
    private String providerName;
    /** Sale units that left stock during the period. */
    private Integer unitsOut;
    private Integer unitsOnHand;
    /**
     * Times the current stock was sold over the period. Measured against the
     * stock on hand today, not against an average: the system keeps no stock
     * history, so this reads as a ratio rather than a true accounting turnover.
     */
    private Double turns;
    /** Days the current stock covers at the sales rate of the period. */
    private Double daysOfSupply;
    private Double costValue;
}
