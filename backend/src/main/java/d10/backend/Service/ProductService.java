package d10.backend.Service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import d10.backend.DTO.Product.CreateProductDTO;
import d10.backend.DTO.Product.PartialProductDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.ProductMapper;
import d10.backend.Model.Product;
import d10.backend.Repository.ProductPaginationRepository;
import d10.backend.Repository.ProductRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPaginationRepository productPaginationRepository;

    public Page<PartialProductDTO> getPaginatedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productPaginationRepository.findAll(pageable)
                .map(ProductMapper::toPartialDTO);
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

}
