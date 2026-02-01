package d10.backend.Controller;

import d10.backend.DTO.Product.CreateProductDTO;
import d10.backend.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(productService.getPaginatedProducts(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody CreateProductDTO product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

}
