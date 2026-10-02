package d10.backend.DTO.Invoice;

import d10.backend.Model.CashRegister;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Money received for a sale, sent with the sale itself so both are saved in
 * one request and the cash register transaction is linked to the sale.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaymentDTO {
    /** What goes into the register, in that register's currency. */
    private Double amount;
    private CashRegister.CashRegisterType registerType;
    /**
     * When false the sale is settled even if {@code amount} is below what is
     * owed (an amount rounded down on the spot). When true only the amount
     * received counts and the rest stays as a debt.
     */
    private Boolean partial;
    /**
     * For a partial payment in USD: how many pesos it covers. Unused
     * otherwise, since peso amounts count as they are.
     */
    private Double pesoAmount;
}
