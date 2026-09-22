package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceivableBucketDTO {
    /** Age range in days, for example 0-30. */
    private String bucket;
    private Integer invoiceCount;
    private Double outstanding;
    private Integer oldestDays;
}
