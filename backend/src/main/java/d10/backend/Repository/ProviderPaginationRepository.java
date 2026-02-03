package d10.backend.Repository;

import d10.backend.Model.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProviderPaginationRepository extends MongoRepository<Provider, String> {
    @Override
    Page<Provider> findAll(Pageable pageable);
}
