package d10.backend.DTO.Shipment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateShipmentDTO {
    private String clientName;
    private String address;
    private String city;
    private String phone;
    private String invoice;
    private Double finalAmount;
    private String details;
    private boolean claim;
    private String shipmentDate;
}
