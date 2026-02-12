package d10.backend.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Product;

@Repository
public interface ProductPaginationRepository extends MongoRepository<Product, String> {

    @Override
    @Query(value = "{}", sort = "{ 'providerName': 1 }")
    Page<Product> findAll(Pageable pageable);

    @Query(
            value = "{ $or: [ "
            + "{ 'name': { $regex: ?0, $options: 'i' } }, "
            + "{ 'code': { $regex: ?0, $options: 'i' } }, "
            + "{ 'description': { $regex: ?0, $options: 'i' } } "
            + "{ 'providerName': { $regex: ?0, $options: 'i' } } "
            + "] }",
            sort = "{ 'providerName': 1 }"
    )
    Page<Product> findBySearchQuery(String query, Pageable pageable);

}
