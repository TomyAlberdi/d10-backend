package d10.backend.Controller;

import d10.backend.DTO.CashRegister.CashRegisterDTO;
import d10.backend.DTO.CashRegister.CashRegisterTransactionDTO;
import d10.backend.DTO.CashRegister.CreateCashRegisterTransactionDTO;
import d10.backend.Service.CashRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/cash-register")
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    @GetMapping
    public ResponseEntity<CashRegisterDTO> getCurrentAmount() {
        return ResponseEntity.ok(cashRegisterService.getCurrentAmount());
    }

    @PostMapping("/transactions")
    public ResponseEntity<CashRegisterTransactionDTO> createTransaction(
            @RequestBody CreateCashRegisterTransactionDTO dto) {
        return ResponseEntity.ok(cashRegisterService.createTransaction(dto));
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<CashRegisterTransactionDTO> updateTransaction(
            @PathVariable String id,
            @RequestBody CreateCashRegisterTransactionDTO dto) {
        return ResponseEntity.ok(cashRegisterService.updateTransaction(id, dto));
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable String id) {
        cashRegisterService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<CashRegisterTransactionDTO>> listTransactionsByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(cashRegisterService.listTransactionsByDate(date));
    }

}

