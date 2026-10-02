package d10.backend.Exception;

import java.util.List;

import d10.backend.DTO.Product.StockShortageDTO;
import lombok.Getter;

/**
 * Thrown when a stock movement would leave one or more products below zero and
 * the caller did not allow it. Carries every affected product, so the user can
 * see them all in a single confirmation.
 */
@Getter
public class InsufficientStockException extends RuntimeException {

    private final List<StockShortageDTO> shortages;

    public InsufficientStockException(List<StockShortageDTO> shortages) {
        super("Stock insuficiente para " + shortages.size() + " producto(s).");
        this.shortages = shortages;
    }
}
