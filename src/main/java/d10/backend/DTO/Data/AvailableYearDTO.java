package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A year that actually holds invoices, so the year selector in the analytics
 * page stops being a hardcoded button and keeps working next January.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AvailableYearDTO {
    private Integer year;
    private Integer invoiceCount;
}
