package d10.backend.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Client;

@Repository
public interface ClientRepository extends MongoRepository<Client, String> {
	java.util.List<Client> findByCuitDniContainingIgnoreCaseOrNameContainingIgnoreCase(String cuitDni, String name);
}
