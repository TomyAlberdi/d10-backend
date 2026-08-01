package d10.backend.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import d10.backend.DTO.StockLog.CreateStockLogDTO;
import d10.backend.DTO.StockLog.StockLogDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.StockLogMapper;
import d10.backend.Model.Product;
import d10.backend.Model.StockLog;
import d10.backend.Repository.ProductRepository;
import d10.backend.Repository.StockLogRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class StockLogService {

    public static final int DEFAULT_PAGE_SIZE = 25;

    private static final Sort MOST_RECENT_FIRST = Sort.by("datetime").descending();

    // The product repository is used instead of the product service to keep this
    // service free of circular dependencies: ProductService registers stock logs.
    private final StockLogRepository stockLogRepository;
    private final ProductRepository productRepository;

    /**
     * Most recent stock logs first, optionally filtered by movement type.
     */
    public Page<StockLogDTO> getPaginatedStockLogs(StockLog.StockLogType type, int page, int size) {
        if (size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }
        Pageable pageable = PageRequest.of(page, size, MOST_RECENT_FIRST);
        Page<StockLog> stockLogs = (type != null)
                ? stockLogRepository.findByType(type, pageable)
                : stockLogRepository.findAll(pageable);
        return stockLogs.map(StockLogMapper::toDTO);
    }

    public StockLogDTO findById(String id) {
        return StockLogMapper.toDTO(findEntityById(id));
    }

    public List<StockLogDTO> findByProductId(String productId) {
        return stockLogRepository.findByProductId(productId, MOST_RECENT_FIRST).stream()
                .map(StockLogMapper::toDTO)
                .collect(Collectors.toList());
    }

    public StockLogDTO createStockLog(CreateStockLogDTO createStockLogDTO) {
        if (createStockLogDTO.getType() == null) {
            throw new IllegalArgumentException("El tipo de movimiento de stock es obligatorio.");
        }
        if (createStockLogDTO.getSaleUnitQuantity() == null || createStockLogDTO.getSaleUnitQuantity() <= 0) {
            throw new IllegalArgumentException("La cantidad del movimiento de stock debe ser mayor a 0.");
        }
        Optional<Product> productSearch = productRepository.findById(createStockLogDTO.getProductId());
        if (productSearch.isEmpty()) {
            throw new ResourceNotFoundException("Producto con ID " + createStockLogDTO.getProductId() + " no encontrado.");
        }
        StockLog stockLog = StockLogMapper.toEntity(
                productSearch.get(),
                createStockLogDTO.getType(),
                createStockLogDTO.getSaleUnitQuantity(),
                createStockLogDTO.getDetail(),
                createStockLogDTO.getDatetime());
        return StockLogMapper.toDTO(stockLogRepository.save(stockLog));
    }

    /**
     * Registers a movement for a product whose stock was already updated.
     * Used by the services that actually move stock (manual updates and invoices).
     */
    public StockLog registerMovement(Product product, StockLog.StockLogType type, Integer saleUnitQuantity,
            String detail, LocalDate date) {
        StockLog stockLog = StockLogMapper.toEntity(product, type, saleUnitQuantity, detail, resolveDatetime(date));
        return stockLogRepository.save(stockLog);
    }

    public void deleteStockLog(String id) {
        findEntityById(id);
        stockLogRepository.deleteById(id);
    }

    private StockLog findEntityById(String id) {
        Optional<StockLog> stockLogSearch = stockLogRepository.findById(id);
        if (stockLogSearch.isEmpty()) {
            throw new ResourceNotFoundException("Movimiento de stock con ID " + id + " no encontrado.");
        }
        return stockLogSearch.get();
    }

    /**
     * Movements carry a plain date (the invoice date, for instance). Same day
     * movements keep the exact time so the log stays ordered, while back dated
     * ones are placed at the start of their own day.
     */
    private LocalDateTime resolveDatetime(LocalDate date) {
        if (date == null || date.isEqual(LocalDate.now())) {
            return LocalDateTime.now();
        }
        return date.atStartOfDay();
    }

}
