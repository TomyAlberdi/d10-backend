package d10.backend.DTO.Pack;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PackItemDTO {
    private String productId;
    private String productName;
    private Double quantity;
}
