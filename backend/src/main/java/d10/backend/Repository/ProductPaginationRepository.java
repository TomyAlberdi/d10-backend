package d10.backend.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Product;

@Repository
public interface ProductPaginationRepository extends MongoRepository<Product, String> {

    @Override
    Page<Product> findAll(Pageable pageable);

}
