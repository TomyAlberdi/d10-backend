package d10.backend.Mapper;

import java.time.LocalDateTime;

import d10.backend.DTO.Note.CreateNoteDTO;
import d10.backend.Model.Note;

public class NoteMapper {

    public static Note toEntity(CreateNoteDTO dto) {
        Note note = new Note();
        note.setBody(dto.getBody());
        note.setDueDate(dto.getDueDate());
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return note;
    }

    public static void updateFromDTO(Note note, CreateNoteDTO dto) {
        note.setBody(dto.getBody());
        note.setDueDate(dto.getDueDate());
        note.setUpdatedAt(LocalDateTime.now());
    }

}
