package d10.backend.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Contact;

@Repository
public interface ContactPaginationRepository extends MongoRepository<Contact, String> {

    @Override
    @Query(value = "{}", sort = "{ 'name': 1 }")
    Page<Contact> findAll(Pageable pageable);

    @Query(value = "{ 'type': ?0 }", sort = "{ 'name': 1 }")
    Page<Contact> findByType(String type, Pageable pageable);

    @Query(value = "{ 'name': { $regex: ?0, $options: 'i' } }", sort = "{ 'name': 1 }")
    Page<Contact> findByNameSearch(String query, Pageable pageable);

    @Query(
            value = "{ '$and': [ "
            + "{ 'name': { $regex: ?0, $options: 'i' } }, "
            + "{ 'type': ?1 } "
            + "] }",
            sort = "{ 'name': 1 }"
    )
    Page<Contact> findByNameSearchAndType(String query, String type, Pageable pageable);

}
