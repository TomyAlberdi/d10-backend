package d10.backend.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "providers")
public class Provider {

    @Id
    private String id;
    private String name;
    private String city;
    private String address;
    private String phone;
    private String email;
    private String cuit;

}
