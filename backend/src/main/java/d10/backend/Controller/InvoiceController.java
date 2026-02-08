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

import d10.backend.DTO.Invoice.CreateInvoiceDTO;
import d10.backend.Model.Invoice;
import d10.backend.Service.InvoiceService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/invoice")
public class InvoiceController {

    private final InvoiceService invoiceService;

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

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(invoiceService.searchInvoices(q));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestParam(name = "status", required = true) Invoice.Status status) {
        return ResponseEntity.ok(invoiceService.updateInvoiceStatus(id, status));
    }

}
