package d10.backend.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Provider;

@Repository
public interface ProviderRepository extends MongoRepository<Provider, String> {
    Optional<Provider> findByName(String name);
}
