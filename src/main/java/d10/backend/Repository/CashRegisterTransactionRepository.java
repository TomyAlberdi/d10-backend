package d10.backend.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.CashRegister;
import d10.backend.Model.CashRegisterTransaction;

@Repository
public interface CashRegisterTransactionRepository extends MongoRepository<CashRegisterTransaction, String> {

    List<CashRegisterTransaction> findByDateTimeBetweenOrderByDateTimeAsc(LocalDateTime start, LocalDateTime end);

    List<CashRegisterTransaction> findByDateTimeBetweenAndRegisterTypeOrderByDateTimeAsc(LocalDateTime start, LocalDateTime end, CashRegister.CashRegisterType registerType);

    Page<CashRegisterTransaction> findByDateTimeBetweenOrderByDateTimeAsc(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<CashRegisterTransaction> findByDateTimeBetweenAndRegisterTypeOrderByDateTimeAsc(LocalDateTime start, LocalDateTime end, CashRegister.CashRegisterType registerType, Pageable pageable);

    Page<CashRegisterTransaction> findAllByOrderByDateTimeAsc(Pageable pageable);

    Page<CashRegisterTransaction> findByRegisterTypeOrderByDateTimeAsc(CashRegister.CashRegisterType registerType, Pageable pageable);

}

