package d10.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import d10.backend.DTO.SortByEnum;
import d10.backend.DTO.TimeSpanEnum;
import d10.backend.Service.DataService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/data")
public class DataController {

    private final DataService dataService;

    /**
     * Get yearly sales data with monthly summaries
     */
    @GetMapping("/yearly-sales/{year}")
    public ResponseEntity<?> getYearlySalesData(@PathVariable Integer year) {
        return ResponseEntity.ok(dataService.getYearlySalesData(year));
    }

    /**
     * Get data analysis results from Product model
     */
    @GetMapping("/product-analysis")
    public ResponseEntity<?> getProductAnalysis() {
        return ResponseEntity.ok(dataService.getProductAnalysis());
    }

    /**
     * Get data analysis results from Client model
     */
    @GetMapping("/client-analysis")
    public ResponseEntity<?> getClientAnalysis() {
        return ResponseEntity.ok(dataService.getClientAnalysis());
    }

    /**
     * Get data analysis results from Invoice model
     */
    @GetMapping("/invoice-analysis")
    public ResponseEntity<?> getInvoiceAnalysis() {
        return ResponseEntity.ok(dataService.getInvoiceAnalysis());
    }

    /**
     * Get data analysis results from CashRegister model
     */
    @GetMapping("/cash-register-analysis")
    public ResponseEntity<?> getCashRegisterAnalysis() {
        return ResponseEntity.ok(dataService.getCashRegisterAnalysis());
    }

    /**
     * Get data analysis results from Warehouse model
     */
    @GetMapping("/warehouse-analysis")
    public ResponseEntity<?> getWarehouseAnalysis() {
        return ResponseEntity.ok(dataService.getWarehouseAnalysis());
    }

    /**
     * Get data analysis results from Provider model
     */
    @GetMapping("/provider-analysis")
    public ResponseEntity<?> getProviderAnalysis() {
        return ResponseEntity.ok(dataService.getProviderAnalysis());
    }

    /**
     * Get comprehensive data analysis from all models
     */
    @GetMapping("/comprehensive")
    public ResponseEntity<?> getComprehensiveAnalysis() {
        return ResponseEntity.ok(dataService.getComprehensiveAnalysis());
    }

    /**
     * Get the 15 best selling products for a given time span and sort criteria
     */
    @GetMapping("/best-selling-products/{timeSpan}/{sortBy}")
    public ResponseEntity<?> getBestSellingProducts(@PathVariable TimeSpanEnum timeSpan, @PathVariable SortByEnum sortBy) {
        return ResponseEntity.ok(dataService.getBestSellingProducts(timeSpan, sortBy));
    }
}
