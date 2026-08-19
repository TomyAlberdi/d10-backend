package d10.backend.DTO.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySalesRecordDTO {
    private Integer year;
    private Integer month;
    private Double income;
    private Integer invoiceCount;
    private Integer unitsSold;
    private Double surfaceSold;
    private Double avgTicket;
}
