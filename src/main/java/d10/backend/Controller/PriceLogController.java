package d10.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import d10.backend.Service.PriceLogService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/price-log")
public class PriceLogController {

    private final PriceLogService priceLogService;

    /**
     * Price changes of a product, most recent first.
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(priceLogService.findByProductId(productId));
    }

}
