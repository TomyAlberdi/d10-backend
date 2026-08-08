package d10.backend.Controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
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

    /**
     * Orders, most recent batch first. Both the batch date and the received flag
     * are optional filters.
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "received", required = false) Boolean received) {
        return ResponseEntity.ok(orderService.findAll(date, received));
    }

    /** Dates that have at least one order, most recent first. */
    @GetMapping("/dates")
    public ResponseEntity<?> getOrderDates() {
        return ResponseEntity.ok(orderService.getOrderDates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(orderService.findByProductId(productId));
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
     * Receives every pending order of a batch. The ordered sale units are added
     * to the stock of each product.
     */
    @PatchMapping("/received")
    public ResponseEntity<?> receiveByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(orderService.receiveByDate(date));
    }

    /**
     * Receives a single order, or sets it back to pending taking its sale units
     * out of the stock again.
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
