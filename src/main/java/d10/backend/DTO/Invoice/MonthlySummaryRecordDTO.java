package d10.backend.DTO.Invoice;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonthlySummaryRecordDTO {
    private Integer month;
    private Integer year;
    private BigDecimal income;
}
