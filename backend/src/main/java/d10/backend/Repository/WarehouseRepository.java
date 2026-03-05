package d10.backend.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Warehouse;

@Repository
public interface WarehouseRepository extends MongoRepository<Warehouse, String> {
    // nothing extra yet; we always operate on the single warehouse document
}
