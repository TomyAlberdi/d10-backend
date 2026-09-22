package d10.backend.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.StockLog;

@Repository
public interface StockLogRepository extends MongoRepository<StockLog, String> {

    Page<StockLog> findByType(StockLog.StockLogType type, Pageable pageable);

    List<StockLog> findByProductId(String productId, Sort sort);

}
