package d10.backend.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import d10.backend.DTO.Product.CreateProductDTO;
import d10.backend.DTO.Product.StockShortageDTO;
import d10.backend.Exception.InsufficientStockException;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.ProductMapper;
import d10.backend.Model.PriceLog;
import d10.backend.Model.Product;
import d10.backend.Model.ProductStock;
import d10.backend.Model.StockLog;
import d10.backend.Repository.ProductPaginationRepository;
import d10.backend.Repository.ProductRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPaginationRepository productPaginationRepository;
    private final StockLogService stockLogService;
    private final PriceLogService priceLogService;

    public Page<Product> getPaginatedProducts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (query != null && !query.trim().isEmpty()) {
            return productPaginationRepository.findBySearchQuery(query, pageable);
        } else {
            return productPaginationRepository.findAll(pageable);
        }
    }

    public Page<Product> getDiscontinuedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productPaginationRepository.findDiscontinuedProducts(pageable);
    }

    public List<Product> getProductsWithStockGreaterThanZero() {
        return productRepository.findByStockQuantityGreaterThan(0);
    }

    public Product findById(String id) {
        Optional<Product> productSearch = productRepository.findById(id);
        if (productSearch.isEmpty()) {
            throw new ResourceNotFoundException("Producto con ID " + id + " no encontrado.");
        }
        Product product = productSearch.get();
        return product;
    }

    /**
     * Resolves several products in a single query, keyed by id. Missing ids
     * are simply absent from the map instead of raising, so callers that only
     * enrich data can skip what is no longer in the catalog.
     */
    public Map<String, Product> findByIds(Collection<String> ids) {
        Map<String, Product> products = new HashMap<>();
        productRepository.findAllById(ids).forEach(product -> products.put(product.getId(), product));
        return products;
    }

    public Product createProduct(CreateProductDTO createProductDTO) {
        Product product = ProductMapper.toEntity(createProductDTO);
        productRepository.save(product);
        priceLogService.registerChange(product, null, PriceLog.PriceLogSource.CREATED, null);
        return product;
    }

    public Product updateProduct(String id, CreateProductDTO createProductDTO) {
        Product product = findById(id);
        PriceLog.PriceSnapshot previousPrices = PriceLog.PriceSnapshot.of(product);
        ProductMapper.setCommercialProductFields(product, createProductDTO);
        ProductMapper.updateFromDTO(product, createProductDTO);
        productRepository.save(product);
        priceLogService.registerChange(product, previousPrices, PriceLog.PriceLogSource.EDITED, null);
        return product;
    }

    public void deleteProduct(String id) {
        findById(id);
        productRepository.deleteById(id);
    }

    public Product updateDiscontinued(String id, Boolean discontinued) {
        Product product = findById(id);
        product.setDiscontinued(discontinued);
        productRepository.save(product);
        return product;
    }

    /**
     * Fails, unless {@code allowNegativeStock} is set, when taking the given
     * sale units out would leave any product below zero. Every product is
     * checked before failing, so the user confirms them all at once.
     *
     * @param requiredByProduct sale units to take out, keyed by product id;
     * a product on several lines must be summed beforehand
     */
    public void checkNegativeStock(Map<String, Integer> requiredByProduct, boolean allowNegativeStock) {
        if (allowNegativeStock) {
            return;
        }
        List<StockShortageDTO> shortages = new ArrayList<>();
        requiredByProduct.forEach((productId, required) -> {
            if (required == null || required <= 0) {
                return;
            }
            Product product = findById(productId);
            int available = currentQuantity(product);
            if (available < required) {
                shortages.add(new StockShortageDTO(product.getId(), product.getName(),
                        product.getSaleUnitType(), available, required, available - required));
            }
        });
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }
    }

    /** Adds {@code quantity} to the product's entry, for {@link #checkNegativeStock}. */
    public static void addRequired(Map<String, Integer> requiredByProduct, String productId, Integer quantity) {
        if (productId != null && quantity != null) {
            requiredByProduct.merge(productId, quantity, Integer::sum);
        }
    }

    private static int currentQuantity(Product product) {
        ProductStock stock = product.getStock();
        return stock != null && stock.getQuantity() != null ? stock.getQuantity() : 0;
    }

    /**
     * Fails when the product data would break {@link #updateStock} halfway
     * through: a movement adds the amount to the current stock and multiplies
     * the result by the measure per sale unit, so both values have to be there.
     *
     * Callers that move the stock of several products in a row check every one
     * of them before writing the first movement, since there is no transaction
     * to roll the earlier ones back.
     */
    public void checkStockUpdatable(String productId) {
        Product product = findById(productId);
        if (product.getMeasurePerSaleUnit() == null) {
            throw new IllegalStateException("El producto " + product.getName()
                    + " no tiene configurada la medida por unidad de venta.");
        }
        ProductStock stock = product.getStock();
        if (stock != null && stock.getQuantity() == null) {
            throw new IllegalStateException("El producto " + product.getName()
                    + " no tiene una cantidad de stock válida.");
        }
    }

    public Product updateStockDecrease(String productId, int quantity, LocalDate date, String detail) {
        return updateStock(productId, StockLog.StockLogType.OUT, quantity, date, detail);
    }

    public Product updateStockIncrease(String productId, int quantity, LocalDate date, String detail) {
        return updateStock(productId, StockLog.StockLogType.IN, quantity, date, detail);
    }

    /**
     * Manual movement from the stock screen. An OUT that would leave the
     * product below zero needs {@code allowNegativeStock}.
     */
    public Product updateStock(String id, StockLog.StockLogType type, Integer quantity, LocalDate date, String detail,
            boolean allowNegativeStock) {
        if (type == StockLog.StockLogType.OUT) {
            checkNegativeStock(Map.of(id, quantity), allowNegativeStock);
        }
        return updateStock(id, type, quantity, date, detail);
    }

    /**
     * Writes a movement. Stock is allowed to go below zero here: callers that
     * take stock out decide beforehand, through {@link #checkNegativeStock},
     * whether the user agreed to it.
     */
    public Product updateStock(String id, StockLog.StockLogType type, Integer quantity, LocalDate date, String detail) {
        Product product = findById(id);
        ProductStock stock = product.getStock();
        if (stock == null) {
            stock = new ProductStock(0, 0.0);
            product.setStock(stock);
        }
        int current = currentQuantity(product);
        // Update quantity based on movement type
        if (type == StockLog.StockLogType.IN) {
            stock.setQuantity(current + quantity);
        } else if (type == StockLog.StockLogType.OUT) {
            stock.setQuantity(current - quantity);
        }
        // Update measure unit equivalent, negative along with the quantity
        if (product.getMeasurePerSaleUnit() != null) {
            stock.setMeasureUnitEquivalent(stock.getQuantity() * product.getMeasurePerSaleUnit());
        }
        productRepository.save(product);
        // Register the movement in the document based stock log
        stockLogService.registerMovement(product, type, quantity, detail, date != null ? date : LocalDate.now());
        return product;
    }

    public List<String> getDistinctProviders() {
        List<Product> products = productRepository.findDistinctProviders();
        List<String> providers = new ArrayList<>();
        for (Product product : products) {
            String provider = product.getProviderName();
            if (provider != null && !provider.isEmpty() && !providers.contains(provider)) {
                providers.add(provider);
            }
        }
        providers.sort(String::compareTo);
        return providers;
    }

    public List<Product> updateCostsByProvider(String providerName, Double percentageChange) {
        List<Product> products = productRepository.findByProviderName(providerName);
        if (products.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron productos del proveedor " + providerName);
        }

        String priceLogDetail = "Proveedor " + providerName + ": "
                + (percentageChange >= 0 ? "+" : "")
                + java.math.BigDecimal.valueOf(percentageChange).stripTrailingZeros().toPlainString().replace('.', ',') + "%";
        List<Product> updatedProducts = new ArrayList<>();
        for (Product product : products) {
            PriceLog.PriceSnapshot previousPrices = PriceLog.PriceSnapshot.of(product);
            Double currentCost = product.getCostByMeasureUnit();
            Double profitPercentage = product.getProfit();
            
            // Check if both cost and profit have valid values
            boolean hasCost = currentCost != null && currentCost > 0;
            boolean hasProfit = profitPercentage != null && profitPercentage > 0;
            
            if (hasCost && hasProfit) {
                // Calculate new cost with percentage change
                Double newCost = truncateToTwoDecimals(currentCost * (1 + (percentageChange / 100)));
                product.setCostByMeasureUnit(newCost);
                
                // Recalculate prices based on new cost and existing profit
                Double newPriceByMeasureUnit = truncateToTwoDecimals(newCost * (1 + (profitPercentage / 100)));
                product.setPriceByMeasureUnit(newPriceByMeasureUnit);
                
                // Recalculate priceBySaleUnit
                Double measurePerSaleUnit = product.getMeasurePerSaleUnit() != null ? product.getMeasurePerSaleUnit() : 1.0;
                Double newPriceBySaleUnit = truncateToTwoDecimals(newPriceByMeasureUnit * measurePerSaleUnit);
                product.setPriceBySaleUnit(newPriceBySaleUnit);
            } else {
                // If cost or profit is missing/invalid, just apply percentage change to prices
                Double currentPriceByMeasureUnit = product.getPriceByMeasureUnit() != null ? product.getPriceByMeasureUnit() : 0.0;
                if (currentPriceByMeasureUnit > 0) {
                    Double newPriceByMeasureUnit = truncateToTwoDecimals(currentPriceByMeasureUnit * (1 + (percentageChange / 100)));
                    product.setPriceByMeasureUnit(newPriceByMeasureUnit);
                    
                    // Update priceBySaleUnit accordingly
                    Double measurePerSaleUnit = product.getMeasurePerSaleUnit() != null ? product.getMeasurePerSaleUnit() : 1.0;
                    Double newPriceBySaleUnit = truncateToTwoDecimals(newPriceByMeasureUnit * measurePerSaleUnit);
                    product.setPriceBySaleUnit(newPriceBySaleUnit);
                }
            }
            
            // Save the updated product
            productRepository.save(product);
            priceLogService.registerChange(product, previousPrices,
                    PriceLog.PriceLogSource.PROVIDER_UPDATE, priceLogDetail);
            updatedProducts.add(product);
        }
        
        return updatedProducts;
    }

    private static double truncateToTwoDecimals(double value) {
        java.math.BigDecimal bd = java.math.BigDecimal.valueOf(value);
        return bd.setScale(2, java.math.RoundingMode.FLOOR).doubleValue();
    }

}
