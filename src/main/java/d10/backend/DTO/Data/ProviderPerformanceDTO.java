package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPerformanceDTO {
    private String providerName;
    private Double revenue;
    private Double cost;
    private Double margin;
    private Double marginPct;
    private Integer unitsSold;
    /** Distinct products of this provider that sold in the period. */
    private Integer skuCount;
    /** Current stock of every product of this provider, at cost. */
    private Double stockValue;
    private Boolean costBasisEstimated;
}
