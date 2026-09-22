package d10.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import d10.backend.DTO.Pack.CreatePackDTO;
import d10.backend.Service.PackService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pack")
public class PackController {

    private final PackService packService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(packService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(packService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreatePackDTO createPackDTO) {
        return ResponseEntity.ok(packService.createPack(createPackDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody CreatePackDTO createPackDTO) {
        return ResponseEntity.ok(packService.updatePack(id, createPackDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        packService.deletePack(id);
        return ResponseEntity.noContent().build();
    }
}
