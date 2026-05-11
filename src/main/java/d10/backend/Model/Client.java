package d10.backend.Model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "clients")
public class Client {

    @Id
    private String id;
    @NotBlank
    private ClientType type;
    @NotBlank
    private String name;
    private String address;
    private String phone;
    private String email;
    private String cuitDni;

    public enum ClientType {
        CONSUMIDOR_FINAL,
        RESPONSABLE_INSCRIPTO
    }

}
