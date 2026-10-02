package d10.backend.Repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.PriceLog;

@Repository
public interface PriceLogRepository extends MongoRepository<PriceLog, String> {

    List<PriceLog> findByProductId(String productId, Sort sort);

}
