package d10.backend.Mapper;

import d10.backend.DTO.Contact.CreateContactDTO;
import d10.backend.Model.Contact;

public class ContactMapper {

    public static Contact toEntity(CreateContactDTO dto) {
        Contact contact = new Contact();
        updateFromDTO(contact, dto);
        return contact;
    }

    public static void updateFromDTO(Contact contact, CreateContactDTO dto) {
        contact.setName(dto.getName());
        contact.setEmail(dto.getEmail());
        contact.setPhone(dto.getPhone());
        contact.setDetail(dto.getDetail());
        contact.setType(dto.getType() != null ? dto.getType() : Contact.ContactType.PROFESSIONAL);
    }

}
