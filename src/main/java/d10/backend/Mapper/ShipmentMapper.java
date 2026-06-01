package d10.backend.Mapper;

import d10.backend.DTO.Shipment.CreateShipmentDTO;
import d10.backend.Model.Shipment;

public class ShipmentMapper {

    public static Shipment toEntity(CreateShipmentDTO dto) {
        Shipment shipment = new Shipment();
        updateFromDTO(shipment, dto);
        return shipment;
    }

    public static void updateFromDTO(Shipment shipment, CreateShipmentDTO dto) {
        shipment.setClientName(dto.getClientName());
        shipment.setAddress(dto.getAddress());
        shipment.setCity(dto.getCity());
        shipment.setPhone(dto.getPhone());
        shipment.setBillNumber(dto.getBillNumber());
        shipment.setTotalAmount(dto.getTotalAmount());
        shipment.setDetails(dto.getDetails());
    }

}
