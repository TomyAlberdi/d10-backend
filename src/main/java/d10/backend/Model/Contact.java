package d10.backend.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "contacts")
public class Contact {

    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String detail;
    private ContactType type = ContactType.PROFESSIONAL;

    public enum ContactType {
        ARCHITECT,
        DESIGNER,
        PROVIDER,
        PROFESSIONAL
    }

}
