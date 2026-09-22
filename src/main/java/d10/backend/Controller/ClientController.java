package d10.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import d10.backend.DTO.Client.AdjustClientBalanceDTO;
import d10.backend.DTO.Client.CreateClientDTO;
import d10.backend.Model.Client;
import d10.backend.Service.ClientService;
import lombok.RequiredArgsConstructor;

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

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(name = "q", required = true) String q) {
        return ResponseEntity.ok(clientService.searchClients(q));
    }

    @PostMapping("/{id}/balance")
    public ResponseEntity<?> adjustBalance(@PathVariable String id, @RequestBody AdjustClientBalanceDTO dto) {
        if (dto.getAmount() == null || dto.getAmount() <= 0 || dto.getType() == null) {
            throw new IllegalArgumentException("El monto a ajustar debe ser mayor a 0.");
        }
        double delta = dto.getType() == AdjustClientBalanceDTO.AdjustmentType.ADD ? dto.getAmount() : -dto.getAmount();
        Client client = clientService.adjustBalance(id, delta);
        return ResponseEntity.ok(client);
    }

}
