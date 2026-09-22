package d10.backend.DTO.Data;

import java.time.LocalDate;

import d10.backend.Model.Client;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Revenue of one client over a period. Built from the client snapshot embedded
 * in each invoice, so no lookup against the clients collection is needed.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TopClientDTO {
    private String clientId;
    private String clientName;
    private Client.ClientType clientType;
    private Double revenue;
    private Integer invoiceCount;
    private Double avgTicket;
    private LocalDate lastPurchase;
}
