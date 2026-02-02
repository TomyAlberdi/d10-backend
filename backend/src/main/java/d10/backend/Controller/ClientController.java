package d10.backend.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import d10.backend.DTO.Client.CreateClientDTO;
import d10.backend.Service.ClientService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/client")
public class ClientController {

    private final ClientService clientService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody CreateClientDTO createClientDTO) {
        return ResponseEntity.ok(clientService.createClient(createClientDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody CreateClientDTO createClientDTO) {
        return ResponseEntity.ok(clientService.updateClient(id, createClientDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

}
