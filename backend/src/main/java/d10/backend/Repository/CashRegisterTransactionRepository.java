package d10.backend.Repository;

import d10.backend.Model.CashRegisterTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CashRegisterTransactionRepository extends MongoRepository<CashRegisterTransaction, String> {

    List<CashRegisterTransaction> findByDateTimeBetweenOrderByDateTimeAsc(LocalDateTime start, LocalDateTime end);

}

