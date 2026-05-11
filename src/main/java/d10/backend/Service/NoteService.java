package d10.backend.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import d10.backend.DTO.Note.CreateNoteDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.NoteMapper;
import d10.backend.Model.Note;
import d10.backend.Repository.NoteRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    public Note findById(String id) {
        Optional<Note> noteSearch = noteRepository.findById(id);
        if (noteSearch.isEmpty()) {
            throw new ResourceNotFoundException("Nota con ID " + id + " no encontrada.");
        }
        return noteSearch.get();
    }

    public List<Note> findAll() {
        return noteRepository.findAll();
    }

    public Note createNote(CreateNoteDTO createNoteDTO) {
        Note note = NoteMapper.toEntity(createNoteDTO);
        noteRepository.save(note);
        return note;
    }

    public Note updateNote(String id, CreateNoteDTO createNoteDTO) {
        Note note = findById(id);
        NoteMapper.updateFromDTO(note, createNoteDTO);
        noteRepository.save(note);
        return note;
    }

    public void deleteNote(String id) {
        findById(id);
        noteRepository.deleteById(id);
    }

}
