package d10.backend.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Order;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByOrderDate(LocalDate orderDate, Sort sort);

    List<Order> findByOrderDateAndReceived(LocalDate orderDate, Boolean received, Sort sort);

    List<Order> findByReceived(Boolean received, Sort sort);

    List<Order> findByProductId(String productId, Sort sort);

}
