package d10.backend.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import d10.backend.DTO.WarehouseCellDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Model.CellItem;
import d10.backend.Model.Product;
import d10.backend.Model.Warehouse;
import d10.backend.Model.WarehouseCell;
import d10.backend.Repository.ProductRepository;
import d10.backend.Repository.WarehouseRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    // constant for the singleton ID
    private static final String WAREHOUSE_ID = "main";

    // dimensions according to requirement
    private static final int ROWS = 10;
    private static final int COLUMNS = 20;
    private static final int LEVELS = 3;

    /**
     * Retrieves the warehouse. If it does not exist yet, creates one with all
     * cells initialized as empty.
     */
    public Warehouse getWarehouse() {
        Optional<Warehouse> opt = warehouseRepository.findById(WAREHOUSE_ID);
        if (opt.isPresent()) {
            Warehouse wh = opt.get();
            // ensure that missing cells are reported
            fillMissingCells(wh);
            return wh;
        } else {
            Warehouse wh = new Warehouse();
            wh.setId(WAREHOUSE_ID);
            wh.setCells(new ArrayList<>());
            fillMissingCells(wh);
            warehouseRepository.save(wh);
            return wh;
        }
    }

    /**
     * Update or insert the information for a single cell then persist the
     * warehouse document.
     */
    public WarehouseCell updateCell(WarehouseCellDTO dto) {
        // sanity check on coordinates
        if (dto.getRow() < 0 || dto.getRow() >= ROWS
                || dto.getColumn() < 0 || dto.getColumn() >= COLUMNS) {
            throw new IllegalArgumentException("Cell coordinates out of bounds");
        }

        Warehouse wh = getWarehouse();
        // convert dto to model cell
        WarehouseCell cell = toModel(dto);
        // find existing cell with same coordinates
        Optional<WarehouseCell> existingOpt = wh.getCells().stream()
                .filter(c -> c.getRow() == cell.getRow()
                && c.getColumn() == cell.getColumn())
                .findFirst();
        if (existingOpt.isPresent()) {
            WarehouseCell existing = existingOpt.get();
            existing.setItems(cell.getItems());
        } else {
            wh.getCells().add(cell);
        }
        warehouseRepository.save(wh);
        return cell;
    }

    private WarehouseCell toModel(WarehouseCellDTO dto) {
        WarehouseCell cell = new WarehouseCell();
        cell.setRow(dto.getRow());
        cell.setColumn(dto.getColumn());
        List<CellItem> items = new ArrayList<>();
        for (WarehouseCellDTO.WarehouseCellItemDTO item : dto.getItems()) {
            Optional<Product> product = productRepository.findById(item.getProductId());
            if (!product.isPresent()) {
                throw new ResourceNotFoundException("Product not found");
            }
            items.add(new CellItem(product.get(), item.getQuantity()));
        }
        cell.setItems(items);
        return cell;
    }

    /**
     * make sure the warehouse has an entry for every possible coordinate. The
     * rules say we want cells even if they are empty, so this method will
     * append blank cells if missing. It does not remove extra cells, but
     * coordinates outside the allowed range are ignored later by the API.
     */
    private void fillMissingCells(Warehouse wh) {
        List<WarehouseCell> cells = wh.getCells();
        boolean added = false;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLUMNS; c++) {
                for (int l = 0; l < LEVELS; l++) {
                    final int rr = r;
                    final int cc = c;
                    boolean present = cells.stream().anyMatch(cell
                            -> cell.getRow() == rr && cell.getColumn() == cc);
                    if (!present) {
                        WarehouseCell empty = new WarehouseCell(rr, cc, new ArrayList<>());
                        cells.add(empty);
                        added = true;
                    }
                }
            }
        }
        // sort for stable order: row, column, level
        cells.sort(Comparator.comparingInt(WarehouseCell::getRow)
                .thenComparingInt(WarehouseCell::getColumn));
        if (added) {
            warehouseRepository.save(wh);
        }
    }
}
