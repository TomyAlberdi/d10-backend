package d10.backend.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import d10.backend.DTO.Product.CreateProductDTO;
import d10.backend.Exception.InsufficientStockException;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.ProductMapper;
import d10.backend.Model.Product;
import d10.backend.Model.ProductStock;
import d10.backend.Model.ProductStockRecord;
import d10.backend.Repository.ProductPaginationRepository;
import d10.backend.Repository.ProductRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPaginationRepository productPaginationRepository;

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

    public Product createProduct(CreateProductDTO createProductDTO) {
        Product product = ProductMapper.toEntity(createProductDTO);
        productRepository.save(product);
        return product;
    }

    public Product updateProduct(String id, CreateProductDTO createProductDTO) {
        Product product = findById(id);
        ProductMapper.setCommercialProductFields(product, createProductDTO);
        ProductMapper.updateFromDTO(product, createProductDTO);
        productRepository.save(product);
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

    public void checkStockSufficient(String productId, int requiredQuantity) {
        Product product = findById(productId);
        ProductStock stock = product.getStock();
        if (stock == null) {
            throw new InsufficientStockException("Stock insuficiente para producto " + product.getName() + ". Disponible: 0, requerido: " + requiredQuantity);
        }
        Integer quantity = stock.getQuantity();
        int availableQuantity = (quantity != null) ? quantity : 0;
        if (availableQuantity < requiredQuantity) {
            throw new InsufficientStockException("Stock insuficiente para producto " + product.getName() + ". Disponible: " + availableQuantity + ", requerido: " + requiredQuantity);
        }
    }

    public Product updateStockDecrease(String productId, int quantity, LocalDate date) {
        findById(productId);
        ProductStockRecord stockRecord = new ProductStockRecord();
        stockRecord.setType(ProductStockRecord.RecordType.OUT);
        stockRecord.setQuantity(quantity);
        stockRecord.setDate(date != null ? date : LocalDate.now());
        return updateStock(productId, stockRecord);
    }

    public Product updateStock(String id, ProductStockRecord stockRecord) {
        Product product = findById(id);
        ProductStock stock = product.getStock();
        if (stock == null) {
            stock = new ProductStock();
            stock.setQuantity(0);
            stock.setMeasureUnitEquivalent(0.0);
            stock.setRecordList(new ArrayList<>());
            product.setStock(stock);
        }
        // Update quantity based on record type
        if (stockRecord.getType() == ProductStockRecord.RecordType.IN) {
            stock.setQuantity(stock.getQuantity() + stockRecord.getQuantity());
        } else if (stockRecord.getType() == ProductStockRecord.RecordType.OUT) {
            if (stock.getQuantity() < stockRecord.getQuantity()) {
                throw new InsufficientStockException("Stock insuficiente. Cantidad disponible: " + stock.getQuantity() + ", cantidad solicitada: " + stockRecord.getQuantity());
            }
            stock.setQuantity(stock.getQuantity() - stockRecord.getQuantity());
        }
        // Update measure unit equivalent
        if (stock.getQuantity() > 0) {
            Double equivalent = stock.getQuantity() * product.getMeasurePerSaleUnit();
            stock.setMeasureUnitEquivalent(equivalent);
        }
        // Set the date if not provided
        if (stockRecord.getDate() == null) {
            stockRecord.setDate(LocalDate.now());
        }
        // Add record to record list
        stock.getRecordList().add(stockRecord);
        productRepository.save(product);
        return product;
    }



}
