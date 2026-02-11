package d10.backend.Service;

import d10.backend.DTO.CashRegister.CashRegisterDTO;
import d10.backend.DTO.CashRegister.CashRegisterTransactionDTO;
import d10.backend.DTO.CashRegister.CreateCashRegisterTransactionDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.CashRegisterMapper;
import d10.backend.Model.CashRegister;
import d10.backend.Model.CashRegisterTransaction;
import d10.backend.Repository.CashRegisterRepository;
import d10.backend.Repository.CashRegisterTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;
    private final CashRegisterTransactionRepository transactionRepository;

    private CashRegister getOrCreateRegister() {
        Optional<CashRegister> existing = cashRegisterRepository.findById(CashRegister.CASH_REGISTER_ID);
        if (existing.isPresent()) {
            return existing.get();
        }
        CashRegister cashRegister = new CashRegister(CashRegister.CASH_REGISTER_ID, 0.0);
        return cashRegisterRepository.save(cashRegister);
    }

    public CashRegisterDTO getCurrentAmount() {
        return CashRegisterMapper.toDTO(getOrCreateRegister());
    }

    public CashRegisterTransactionDTO createTransaction(CreateCashRegisterTransactionDTO dto) {
        validateAmount(dto.getAmount());
        CashRegister register = getOrCreateRegister();

        double signedAmount = getSignedAmount(dto.getAmount(), dto.getType());
        register.setCurrentAmount(register.getCurrentAmount() + signedAmount);
        cashRegisterRepository.save(register);

        CashRegisterTransaction transaction = CashRegisterMapper.toEntity(dto);
        transaction = transactionRepository.save(transaction);
        return CashRegisterMapper.toDTO(transaction);
    }

    public CashRegisterTransactionDTO updateTransaction(String id, CreateCashRegisterTransactionDTO dto) {
        validateAmount(dto.getAmount());
        CashRegisterTransaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción de caja con ID " + id + " no encontrada."));

        CashRegister register = getOrCreateRegister();

        double oldSigned = getSignedAmount(existing.getAmount(), existing.getType());
        double newSigned = getSignedAmount(dto.getAmount(), dto.getType());

        register.setCurrentAmount(register.getCurrentAmount() - oldSigned + newSigned);
        cashRegisterRepository.save(register);

        CashRegisterMapper.updateFromDTO(existing, dto);
        existing = transactionRepository.save(existing);

        return CashRegisterMapper.toDTO(existing);
    }

    public void deleteTransaction(String id) {
        CashRegisterTransaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción de caja con ID " + id + " no encontrada."));

        CashRegister register = getOrCreateRegister();
        double oldSigned = getSignedAmount(existing.getAmount(), existing.getType());
        register.setCurrentAmount(register.getCurrentAmount() - oldSigned);
        cashRegisterRepository.save(register);

        transactionRepository.deleteById(id);
    }

    public List<CashRegisterTransactionDTO> listTransactionsByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);
        List<CashRegisterTransaction> transactions = transactionRepository
                .findByDateTimeBetweenOrderByDateTimeAsc(start, end);
        return transactions.stream()
                .map(CashRegisterMapper::toDTO)
                .collect(Collectors.toList());
    }

    private void validateAmount(Double amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("El monto de la transacción debe ser mayor a 0.");
        }
    }

    private double getSignedAmount(Double amount, CashRegisterTransaction.TransactionType type) {
        if (type == CashRegisterTransaction.TransactionType.OUT) {
            return -amount;
        }
        return amount;
    }

}

