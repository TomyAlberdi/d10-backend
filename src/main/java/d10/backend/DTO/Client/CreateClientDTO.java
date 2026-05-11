package d10.backend.DTO.Client;

import d10.backend.Model.Client;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateClientDTO {
    private Client.ClientType type;
    private String name;
    private String address;
    private String cuitDni;
    private String email;
    private String phone;
}
