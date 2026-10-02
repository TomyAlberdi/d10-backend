package d10.backend.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import d10.backend.Model.PriceLog;
import d10.backend.Model.Product;
import d10.backend.Repository.PriceLogRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PriceLogService {

    private static final Sort MOST_RECENT_FIRST = Sort.by("datetime").descending();

    private final PriceLogRepository priceLogRepository;

    public List<PriceLog> findByProductId(String productId) {
        return priceLogRepository.findByProductId(productId, MOST_RECENT_FIRST);
    }

    /**
     * Records the pricing a product was just saved with. Nothing is written
     * when the prices are the same as {@code previous}, so editing the name or
     * the description of a product does not add noise to its history.
     *
     * @param previous the prices before the change, null for a new product
     */
    public void registerChange(Product product, PriceLog.PriceSnapshot previous,
            PriceLog.PriceLogSource source, String detail) {
        PriceLog.PriceSnapshot current = PriceLog.PriceSnapshot.of(product);
        if (current.equals(previous)) {
            return;
        }
        priceLogRepository.save(new PriceLog(null, product.getId(), product.getName(),
                product.getMeasureType(), product.getSaleUnitType(), LocalDateTime.now(),
                source, detail, previous, current));
    }

}
