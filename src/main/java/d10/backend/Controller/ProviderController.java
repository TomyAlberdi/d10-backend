package d10.backend.Controller;

import d10.backend.DTO.Product.CreateProductDTO;
import d10.backend.DTO.Provider.CreateProviderDTO;
import d10.backend.Model.Provider;
import d10.backend.Service.ProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/provider")
public class ProviderController {

    private final ProviderService providerService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(providerService.getPaginatedProviders(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(providerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody CreateProviderDTO provider) {
        return ResponseEntity.ok(providerService.createProvider(provider));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody CreateProviderDTO provider) {
        return ResponseEntity.ok(providerService.updateProvider(id, provider));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        providerService.deleteProvider(id);
        return ResponseEntity.noContent().build();
    }

}
