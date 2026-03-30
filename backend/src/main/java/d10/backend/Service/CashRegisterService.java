package d10.backend.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import d10.backend.DTO.CashRegister.CashRegisterDTO;
import d10.backend.DTO.CashRegister.CashRegisterDailyTotalsDTO;
import d10.backend.DTO.CashRegister.CashRegisterTransactionDTO;
import d10.backend.DTO.CashRegister.CreateCashRegisterTransactionDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.CashRegisterMapper;
import d10.backend.Model.CashRegister;
import d10.backend.Model.CashRegisterTransaction;
import d10.backend.Repository.CashRegisterRepository;
import d10.backend.Repository.CashRegisterTransactionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;
    private final CashRegisterTransactionRepository transactionRepository;

    private CashRegister getOrCreateRegister(CashRegister.CashRegisterType type) {
        String id = type == CashRegister.CashRegisterType.PAPER ? CashRegister.PAPER_CASH_REGISTER_ID : CashRegister.DIGITAL_CASH_REGISTER_ID;
        Optional<CashRegister> existing = cashRegisterRepository.findById(id);
        if (existing.isPresent()) {
            return existing.get();
        }
        CashRegister cashRegister = new CashRegister(id, 0.0, type);
        return cashRegisterRepository.save(cashRegister);
    }

    public CashRegisterDTO getCurrentAmount(CashRegister.CashRegisterType type) {
        return CashRegisterMapper.toDTO(getOrCreateRegister(type));
    }

    public CashRegisterTransactionDTO createTransaction(CreateCashRegisterTransactionDTO dto) {
        validateAmount(dto.getAmount());
        CashRegister register = getOrCreateRegister(dto.getRegisterType());

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

        CashRegister register = getOrCreateRegister(dto.getRegisterType());

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

        CashRegister register = getOrCreateRegister(existing.getRegisterType());
        double oldSigned = getSignedAmount(existing.getAmount(), existing.getType());
        register.setCurrentAmount(register.getCurrentAmount() - oldSigned);
        cashRegisterRepository.save(register);

        transactionRepository.deleteById(id);
    }

    public List<CashRegisterTransactionDTO> listTransactionsByDate(LocalDate date, CashRegister.CashRegisterType type) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);
        List<CashRegisterTransaction> transactions;
        if (type != null) {
            transactions = transactionRepository
                    .findByDateTimeBetweenAndRegisterTypeOrderByDateTimeAsc(start, end, type);
        } else {
            transactions = transactionRepository
                    .findByDateTimeBetweenOrderByDateTimeAsc(start, end);
        }
        return transactions.stream()
                .map(CashRegisterMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Calculate aggregated totals for the given date (and optional type filter).
     */
    public CashRegisterDailyTotalsDTO getDailyTotals(LocalDate date, CashRegister.CashRegisterType type) {
        // reuse list logic to avoid duplication
        List<CashRegisterTransactionDTO> list = listTransactionsByDate(date, type);
        double inTotal = 0.0;
        double outTotal = 0.0;
        for (CashRegisterTransactionDTO t : list) {
            if (t.getType() == CashRegisterTransaction.TransactionType.IN) {
                inTotal += t.getAmount() != null ? t.getAmount() : 0;
            } else if (t.getType() == CashRegisterTransaction.TransactionType.OUT) {
                outTotal += t.getAmount() != null ? t.getAmount() : 0;
            }
        }
        return new CashRegisterDailyTotalsDTO(inTotal, outTotal);
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

    /**
     * Returns paginated transactions for a given date range and optional register type.
     * Default page size is 50.
     * If date is null, returns all transactions.
     * Transactions are ordered by date with most recent first.
     */
    public Page<CashRegisterTransactionDTO> listTransactionsPaginated(
            LocalDate date,
            CashRegister.CashRegisterType type,
            int page,
            int size) {
        // Use default size of 50 if not specified or if size is 0
        if (size <= 0) {
            size = 50;
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateTime").descending());

        Page<CashRegisterTransaction> transactions;
        
        // If date is null, fetch all transactions
        if (date == null) {
            if (type != null) {
                transactions = transactionRepository
                        .findByRegisterTypeOrderByDateTimeAsc(type, pageable);
            } else {
                transactions = transactionRepository
                        .findAllByOrderByDateTimeAsc(pageable);
            }
        } else {
            // Otherwise, fetch transactions for the specific date
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);
            if (type != null) {
                transactions = transactionRepository
                        .findByDateTimeBetweenAndRegisterTypeOrderByDateTimeAsc(start, end, type, pageable);
            } else {
                transactions = transactionRepository
                        .findByDateTimeBetweenOrderByDateTimeAsc(start, end, pageable);
            }
        }
        return transactions.map(CashRegisterMapper::toDTO);
    }

}

