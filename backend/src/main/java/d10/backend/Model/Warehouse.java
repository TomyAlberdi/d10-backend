package d10.backend.Model;

import java.util.ArrayList;
import java.util.List;

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
@Document(collection = "warehouse")
public class Warehouse {
    @Id
    private String id;

    /**
     * Flat list of all cells currently stored in the warehouse.  A complete grid
     * will always be returned by the service, filling missing cells with empty
     * item lists.
     */
    private List<WarehouseCell> cells = new ArrayList<>();
}
