package d10.backend.DTO.Pack;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePackDTO {
    private String name;
    private List<PackItemDTO> items;
}
