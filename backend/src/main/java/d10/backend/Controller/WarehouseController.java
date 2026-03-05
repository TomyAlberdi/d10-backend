package d10.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import d10.backend.DTO.WarehouseCellDTO;
import d10.backend.Model.Warehouse;
import d10.backend.Model.WarehouseCell;
import d10.backend.Service.WarehouseService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ResponseEntity<Warehouse> getWarehouse() {
        return ResponseEntity.ok(warehouseService.getWarehouse());
    }

    /**
     * Updates the contents of a single cell. The payload must include
     * row/column/level and a list of product ids and quantities; an empty list
     * clears the cell.
     */
    @PutMapping("/cell")
    public ResponseEntity<WarehouseCell> updateCell(@RequestBody WarehouseCellDTO dto) {
        WarehouseCell updated = warehouseService.updateCell(dto);
        return ResponseEntity.ok(updated);
    }
}
