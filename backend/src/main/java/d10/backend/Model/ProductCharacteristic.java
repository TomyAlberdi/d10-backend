package d10.backend.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductCharacteristic {

    private CharacteristicType key;
    private String value;

    public enum CharacteristicType {
        COLOR, ORIGEN, BORDE, ASPECTO, TEXTURA, TRANSITO
    }

}
