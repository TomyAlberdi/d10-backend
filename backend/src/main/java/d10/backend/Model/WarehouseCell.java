package d10.backend.Model;

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
public class WarehouseCell {
    private int row;
    private int column;
    private int level;
    private List<CellItem> items = new ArrayList<>();
}
