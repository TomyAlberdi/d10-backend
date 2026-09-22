package d10.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import d10.backend.DTO.Shipment.CreateShipmentDTO;
import d10.backend.Service.ShipmentService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/shipment")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam String date) {
        return ResponseEntity.ok(shipmentService.findAll(date));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(shipmentService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody CreateShipmentDTO createShipmentDTO) {
        return ResponseEntity.ok(shipmentService.createShipment(createShipmentDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody CreateShipmentDTO createShipmentDTO) {
        return ResponseEntity.ok(shipmentService.updateShipment(id, createShipmentDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        shipmentService.deleteShipment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(name = "q", required = true) String q) {
        return ResponseEntity.ok(shipmentService.searchShipments(q));
    }

}
