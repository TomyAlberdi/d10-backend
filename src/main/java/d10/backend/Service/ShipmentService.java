package d10.backend.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import d10.backend.DTO.Shipment.CreateShipmentDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.ShipmentMapper;
import d10.backend.Model.Shipment;
import d10.backend.Repository.ShipmentRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    public Shipment findById(String id) {
        Optional<Shipment> shipmentSearch = shipmentRepository.findById(id);
        if (shipmentSearch.isEmpty()) {
            throw new ResourceNotFoundException("Envío con ID " + id + " no encontrado.");
        }
        return shipmentSearch.get();
    }

    public Shipment createShipment(CreateShipmentDTO createShipmentDTO) {
        Shipment shipment = ShipmentMapper.toEntity(createShipmentDTO);
        shipmentRepository.save(shipment);
        return shipment;
    }

    public Shipment updateShipment(String id, CreateShipmentDTO createShipmentDTO) {
        Shipment shipment = findById(id);
        ShipmentMapper.updateFromDTO(shipment, createShipmentDTO);
        shipmentRepository.save(shipment);
        return shipment;
    }

    public List<Shipment> searchShipments(String q) {
        if (q == null) {
            return java.util.Collections.emptyList();
        }
        return shipmentRepository.findByClientNameContainingIgnoreCaseOrBillNumberContainingIgnoreCase(q, q);
    }

    public void deleteShipment(String id) {
        findById(id);
        shipmentRepository.deleteById(id);
    }

}
