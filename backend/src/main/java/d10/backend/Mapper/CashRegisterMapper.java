package d10.backend.Mapper;

import d10.backend.DTO.CashRegister.CashRegisterDTO;
import d10.backend.DTO.CashRegister.CashRegisterTransactionDTO;
import d10.backend.DTO.CashRegister.CreateCashRegisterTransactionDTO;
import d10.backend.Model.CashRegister;
import d10.backend.Model.CashRegisterTransaction;

import java.time.LocalDateTime;

public class CashRegisterMapper {

    private CashRegisterMapper() {
        // Utility class
    }

    public static CashRegisterDTO toDTO(CashRegister cashRegister) {
        if (cashRegister == null) {
            return new CashRegisterDTO(0.0);
        }
        return new CashRegisterDTO(cashRegister.getCurrentAmount());
    }

    public static CashRegisterTransaction toEntity(CreateCashRegisterTransactionDTO dto) {
        CashRegisterTransaction transaction = new CashRegisterTransaction();
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setDescription(dto.getDescription());
        transaction.setDateTime(LocalDateTime.now());
        return transaction;
    }

    public static void updateFromDTO(CashRegisterTransaction transaction, CreateCashRegisterTransactionDTO dto) {
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setDescription(dto.getDescription());
    }

    public static CashRegisterTransactionDTO toDTO(CashRegisterTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new CashRegisterTransactionDTO(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getDateTime()
        );
    }

}

