package d10.backend.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Shipment;

@Repository
public interface ShipmentRepository extends MongoRepository<Shipment, String> {
    List<Shipment> findByClientNameContainingIgnoreCaseOrInvoiceContainingIgnoreCase(String clientName, String invoice);
}
