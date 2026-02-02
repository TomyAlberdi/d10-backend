package d10.backend.Controller;

import d10.backend.DTO.Invoice.CreateInvoiceDTO;
import d10.backend.Service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/invoice")
public class InvoiceController {

    private final InvoiceService  invoiceService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(invoiceService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody CreateInvoiceDTO createInvoiceDTO) {
        return ResponseEntity.ok(invoiceService.createInvoice(createInvoiceDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody CreateInvoiceDTO createInvoiceDTO) {
        return ResponseEntity.ok(invoiceService.updateInvoice(id, createInvoiceDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

}
