package d10.backend.DTO.Provider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateProviderDTO {
    private String name;
    private String city;
    private String address;
    private String phone;
    private String email;
    private String cuit;
}
