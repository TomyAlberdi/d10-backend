package d10.backend.DTO.Contact;

import d10.backend.Model.Contact;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateContactDTO {
    private String name;
    private String email;
    private String phone;
    private String detail;
    private Contact.ContactType type;
}
