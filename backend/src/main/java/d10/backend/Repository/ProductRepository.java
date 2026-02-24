package d10.backend.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCode(String code);
    List<Product> findByName(String name);
    List<Product> findByStockQuantityGreaterThan(Integer quantity);
}
