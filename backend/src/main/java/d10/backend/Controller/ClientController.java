package d10.backend.Controller;

import d10.backend.DTO.Client.CreateClientDTO;
import d10.backend.Service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}
