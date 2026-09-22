package d10.backend.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Order;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

}
