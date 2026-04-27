package d10.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Invoice;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {

	List<Invoice> findByClientCuitDniContainingIgnoreCaseOrClientNameContainingIgnoreCase(String cuitDni, String name);

	List<Invoice> findTop15ByOrderByDateDescInvoiceNumberDesc();

	Optional<Invoice> findTopByOrderByInvoiceNumberDesc();

	List<Invoice> findByStatusOrderByDateDescInvoiceNumberDesc(Invoice.Status status);

	List<Invoice> findByStatusAndClientCuitDniContainingIgnoreCaseOrStatusAndClientNameContainingIgnoreCase(Invoice.Status status, String cuitDni, Invoice.Status status2, String name);

	List<Invoice> findByStockDecreasedFalseOrderByDateDescInvoiceNumberDesc();

	@Query("{ 'products.id': ?0 }")
	List<Invoice> findByProductId(String productId);

	@Query("{ 'date': { $gte: ?0, $lt: ?1 }, 'status': { $in: ?2 } }")
	List<Invoice> findByDateRangeAndStatus(java.time.LocalDate startDate, java.time.LocalDate endDate, java.util.List<Invoice.Status> statuses);

	@Query(value = "{ $or: [ { 'invoiceNumber': { $regex: ?0, $options: 'i' } }, { 'client.cuitDni': { $regex: ?0, $options: 'i' } }, { 'client.name': { $regex: ?0, $options: 'i' } } ] }", sort = "{ 'date': -1, 'invoiceNumber': -1 }")
	List<Invoice> findByInvoiceNumberOrClientCuitDniOrClientName(String query);

	@Query(value = "{ 'status': ?0, $or: [ { 'invoiceNumber': { $regex: ?1, $options: 'i' } }, { 'client.cuitDni': { $regex: ?1, $options: 'i' } }, { 'client.name': { $regex: ?1, $options: 'i' } } ] }", sort = "{ 'date': -1, 'invoiceNumber': -1 }")
	List<Invoice> findByStatusAndInvoiceNumberOrClientCuitDniOrClientName(Invoice.Status status, String query);

}
