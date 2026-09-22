package d10.backend.Service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import d10.backend.DTO.Contact.CreateContactDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.ContactMapper;
import d10.backend.Model.Contact;
import d10.backend.Repository.ContactPaginationRepository;
import d10.backend.Repository.ContactRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final ContactPaginationRepository contactPaginationRepository;

    public Page<Contact> getPaginatedContacts(String query, String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String trimmedQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        Contact.ContactType parsedType = parseType(type);
        String typeName = parsedType != null ? parsedType.name() : null;

        if (trimmedQuery != null && typeName != null) {
            return contactPaginationRepository.findByNameSearchAndType(trimmedQuery, typeName, pageable);
        } else if (trimmedQuery != null) {
            return contactPaginationRepository.findByNameSearch(trimmedQuery, pageable);
        } else if (typeName != null) {
            return contactPaginationRepository.findByType(typeName, pageable);
        } else {
            return contactPaginationRepository.findAll(pageable);
        }
    }

    public Contact findById(String id) {
        Optional<Contact> contactSearch = contactRepository.findById(id);
        if (contactSearch.isEmpty()) {
            throw new ResourceNotFoundException("Contacto con ID " + id + " no encontrado.");
        }
        Contact contact = contactSearch.get();
        return contact;
    }

    public Contact createContact(CreateContactDTO createContactDTO) {
        Contact contact = ContactMapper.toEntity(createContactDTO);
        contactRepository.save(contact);
        return contact;
    }

    public Contact updateContact(String id, CreateContactDTO createContactDTO) {
        Contact contact = findById(id);
        ContactMapper.updateFromDTO(contact, createContactDTO);
        contactRepository.save(contact);
        return contact;
    }

    public void deleteContact(String id) {
        findById(id);
        contactRepository.deleteById(id);
    }

    private Contact.ContactType parseType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        try {
            return Contact.ContactType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
