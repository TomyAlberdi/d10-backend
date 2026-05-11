package d10.backend.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import d10.backend.Model.Note;

@Repository
public interface NoteRepository extends MongoRepository<Note, String> {
}
