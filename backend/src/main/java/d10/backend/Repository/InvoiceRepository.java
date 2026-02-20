package d10.backend.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Invoice;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {

	java.util.List<Invoice> findByClientCuitDniContainingIgnoreCaseOrClientNameContainingIgnoreCase(String cuitDni, String name);

	java.util.List<Invoice> findTop15ByOrderByDateDesc();

	Optional<Invoice> findTopByOrderByInvoiceNumberDesc();

}
