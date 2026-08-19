package d10.backend.Controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import d10.backend.DTO.RevenueBasisEnum;
import d10.backend.DTO.SortByEnum;
import d10.backend.DTO.TimeSpanEnum;
import d10.backend.Model.CashRegister;
import d10.backend.Service.DataService;
import lombok.RequiredArgsConstructor;

/**
 * Analytics endpoints.
 *
 * Every sales figure takes an optional {@code basis} telling it which invoice
 * statuses count as revenue, so two charts on the same screen can no longer
 * disagree about the same month. COLLECTED (the default) is money actually
 * taken; DELIVERED adds DEUDA, the sales that left the building unpaid;
 * QUOTED adds PENDIENTE and is for demand analysis only.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/data")
public class DataController {

    private final DataService dataService;

    /**
     * Monthly income of a year, zero filled.
     */
    @GetMapping("/yearly-sales/{year}")
    public ResponseEntity<?> getYearlySalesData(
            @PathVariable Integer year,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getYearlySalesData(year, basis));
    }

    /**
     * Years that hold at least one invoice, most recent first. Lets the year
     * selector be built from the data instead of hardcoded.
     */
    @GetMapping("/available-years")
    public ResponseEntity<?> getAvailableYears() {
        return ResponseEntity.ok(dataService.getAvailableYears());
    }

    /**
     * The 15 best selling products for a time span and sort criteria.
     */
    @GetMapping("/best-selling-products/{timeSpan}/{sortBy}")
    public ResponseEntity<?> getBestSellingProducts(
            @PathVariable TimeSpanEnum timeSpan,
            @PathVariable SortByEnum sortBy,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getBestSellingProducts(timeSpan, sortBy, basis));
    }

    /**
     * The 5 best selling products of a category.
     */
    @GetMapping("/top-by-category")
    public ResponseEntity<?> getTop5ByCategory(
            @RequestParam(value = "category") String category,
            @RequestParam(value = "sortBy") SortByEnum sortBy,
            @RequestParam(value = "timespan") TimeSpanEnum timespan,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getTop5ByCategory(category, sortBy, timespan, basis));
    }

    /**
     * The 5 best selling products of a subcategory.
     */
    @GetMapping("/top-by-subcategory")
    public ResponseEntity<?> getTop5BySubcategory(
            @RequestParam(value = "subcategory") String subcategory,
            @RequestParam(value = "sortBy") SortByEnum sortBy,
            @RequestParam(value = "timespan") TimeSpanEnum timespan,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getTop5BySubcategory(subcategory, sortBy, timespan, basis));
    }

    /**
     * Monthly cash in, cash out and net movement of a year.
     *
     * @param registerType optional; omit to total every register, bearing in
     * mind that USD holds a different currency than PAPER and DIGITAL.
     */
    @GetMapping("/cash-flow/monthly")
    public ResponseEntity<?> getMonthlyCashFlow(
            @RequestParam(value = "year") Integer year,
            @RequestParam(value = "registerType", required = false) CashRegister.CashRegisterType registerType) {
        return ResponseEntity.ok(dataService.getMonthlyCashFlow(year, registerType));
    }

    /**
     * Clients ranked by revenue over a period.
     *
     * @param from inclusive, optional
     * @param to exclusive, optional; omit both for all time
     */
    @GetMapping("/clients/top")
    public ResponseEntity<?> getTopClients(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getTopClients(from, to, limit, basis));
    }

}
