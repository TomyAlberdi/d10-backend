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

import d10.backend.DTO.Order.CreateOrderDTO;
import d10.backend.Service.OrderService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    /** Every order, most recent first. */
    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(orderService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody CreateOrderDTO createOrderDTO) {
        return ResponseEntity.ok(orderService.createOrder(createOrderDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody CreateOrderDTO createOrderDTO) {
        return ResponseEntity.ok(orderService.updateOrder(id, createOrderDTO));
    }

    /**
     * Receives the whole order, adding the ordered sale units to the stock of
     * every product, or sets it back to pending taking them out again.
     */
    @PatchMapping("/{id}/received")
    public ResponseEntity<?> updateReceived(
            @PathVariable String id,
            @RequestParam(value = "received", defaultValue = "true") boolean received) {
        return ResponseEntity.ok(orderService.updateReceived(id, received));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

}
