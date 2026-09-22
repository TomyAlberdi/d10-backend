package d10.backend.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "shipments")
public class Shipment {

    @Id
    private String id;
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
