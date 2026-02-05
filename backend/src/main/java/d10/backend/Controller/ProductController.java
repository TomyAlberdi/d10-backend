package d10.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import d10.backend.DTO.Product.CreateProductDTO;
import d10.backend.Model.ProductStockRecord;
import d10.backend.Service.ProductService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(productService.getPaginatedProducts(query, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody CreateProductDTO product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody CreateProductDTO product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/discontinued")
    public ResponseEntity<?> updateDiscontinued(
            @PathVariable String id,
            @RequestParam(value = "discontinued") Boolean discontinued) {
        return ResponseEntity.ok(productService.updateDiscontinued(id, discontinued));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(
            @PathVariable String id,
            @RequestBody ProductStockRecord stockRecord) {
        return ResponseEntity.ok(productService.updateStock(id, stockRecord));
    }


}
