package d10.backend.Repository;

import d10.backend.Model.CashRegister;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashRegisterRepository extends MongoRepository<CashRegister, String> {
}

