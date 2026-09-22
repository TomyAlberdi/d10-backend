package d10.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import d10.backend.DTO.StockLog.CreateStockLogDTO;
import d10.backend.Model.StockLog;
import d10.backend.Service.StockLogService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/stock-log")
public class StockLogController {

    private final StockLogService stockLogService;

    /**
     * Paginated stock movements, most recent first.
     * Default page size: 25. Optional IN/OUT filter.
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(value = "type", required = false) StockLog.StockLogType type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {
        return ResponseEntity.ok(stockLogService.getPaginatedStockLogs(type, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(stockLogService.findById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(stockLogService.findByProductId(productId));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody CreateStockLogDTO createStockLogDTO) {
        return ResponseEntity.ok(stockLogService.createStockLog(createStockLogDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        stockLogService.deleteStockLog(id);
        return ResponseEntity.noContent().build();
    }

}
