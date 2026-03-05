package d10.backend.DTO;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseCellDTO {
    private int row;
    private int column;
    private int level;
    private List<WarehouseCellItemDTO> items = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseCellItemDTO {
        private String productId;
        private Integer quantity;
    }
}
