package d10.backend.Repository;

import d10.backend.Model.Provider;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderRepository extends MongoRepository<Provider, String> {
}
