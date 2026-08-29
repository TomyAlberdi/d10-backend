package d10.backend.Controller;

import java.time.LocalDate;
import java.util.List;

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
import d10.backend.Service.StockAnalyticsService;
import lombok.RequiredArgsConstructor;

/**
 * Analytics endpoints.
 *
 * Every sales figure takes an optional {@code basis} telling it which invoice
 * statuses count as revenue, so two charts on the same screen can no longer
 * disagree about the same month. COLLECTED (the default) is money actually
 * taken; DELIVERED adds DEUDA, the sales that left the building unpaid;
 * QUOTED adds PENDIENTE and is for demand analysis only.
 *
 * Contract: monthly series take a {@code year}, everything else takes an
 * optional {@code from}/{@code to} range.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/data")
public class DataController {

    private final DataService dataService;
    private final StockAnalyticsService stockAnalyticsService;

    // ---------------------------------------------------------------- sales

    /**
     * Monthly income of a year, zero filled.
     *
     * Income is the money the month collected: the invoices the basis counts
     * plus whatever has already been paid on the ones still owing, reported
     * apart as {@code debtPayments} so the chart can show both.
     */
    @GetMapping("/yearly-sales/{year}")
    public ResponseEntity<?> getYearlySalesData(
            @PathVariable Integer year,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getYearlySalesData(year, basis));
    }

    /**
     * Monthly sales of one or more years, for a year over year comparison.
     *
     * @param years comma separated, e.g. {@code ?years=2025,2026}
     */
    @GetMapping("/sales/monthly")
    public ResponseEntity<?> getMonthlySales(
            @RequestParam(value = "years", required = false) List<Integer> years,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getMonthlySales(years, basis));
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
     * Headline figures of a period, each against the preceding period of equal
     * length. Omit the range for the current year to date.
     */
    @GetMapping("/kpi/summary")
    public ResponseEntity<?> getKpiSummary(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getKpiSummary(from, to, basis));
    }

    /**
     * Revenue split across the catalog, by category or subcategory.
     */
    @GetMapping("/revenue/by-category")
    public ResponseEntity<?> getRevenueByCategory(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "level", defaultValue = "CATEGORY") DataService.CategoryLevelEnum level,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getRevenueByCategory(from, to, level, basis));
    }

    /**
     * Revenue by payment method, month by month.
     */
    @GetMapping("/revenue/by-payment-method")
    public ResponseEntity<?> getRevenueByPaymentMethod(
            @RequestParam(value = "year") Integer year,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getRevenueByPaymentMethod(year, basis));
    }

    // ------------------------------------------------------- product ranking

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

    // ---------------------------------------------------------- receivables

    /**
     * Outstanding balance on delivered but unpaid sales, by age in days.
     */
    @GetMapping("/receivables/aging")
    public ResponseEntity<?> getReceivablesAging() {
        return ResponseEntity.ok(dataService.getReceivablesAging());
    }

    /**
     * Clients ranked by what they still owe.
     */
    @GetMapping("/receivables/top-clients")
    public ResponseEntity<?> getTopDebtors(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(dataService.getTopDebtors(limit));
    }

    // ----------------------------------------------------------- cash / clients

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
     */
    @GetMapping("/clients/top")
    public ResponseEntity<?> getTopClients(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getTopClients(from, to, limit, basis));
    }

    // --------------------------------------------------------------- suppliers

    /**
     * Revenue, margin and immobilised stock per supplier.
     */
    @GetMapping("/providers/performance")
    public ResponseEntity<?> getProviderPerformance(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "basis", defaultValue = "COLLECTED") RevenueBasisEnum basis) {
        return ResponseEntity.ok(dataService.getProviderPerformance(from, to, basis));
    }

    // ------------------------------------------------------------------- stock

    /**
     * Stock on hand valued at cost and at list price.
     */
    @GetMapping("/stock/valuation")
    public ResponseEntity<?> getStockValuation(
            @RequestParam(value = "groupBy", defaultValue = "CATEGORY") StockAnalyticsService.StockGroupByEnum groupBy) {
        return ResponseEntity.ok(stockAnalyticsService.getStockValuation(groupBy));
    }

    /**
     * How many times each product sold through its stock over the period.
     */
    @GetMapping("/stock/turnover")
    public ResponseEntity<?> getStockTurnover(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "limit", defaultValue = "40") Integer limit) {
        return ResponseEntity.ok(stockAnalyticsService.getStockTurnover(from, to, limit));
    }

    /**
     * Products holding stock that have not moved out in the given number of days.
     */
    @GetMapping("/stock/dead")
    public ResponseEntity<?> getDeadStock(
            @RequestParam(value = "daysWithoutSale", defaultValue = "90") Integer daysWithoutSale) {
        return ResponseEntity.ok(stockAnalyticsService.getDeadStock(daysWithoutSale));
    }

    // ----------------------------------------------------------------- catalog

    /**
     * How much of the catalog is missing the fields the other charts rely on.
     */
    @GetMapping("/catalog/quality")
    public ResponseEntity<?> getCatalogQuality() {
        return ResponseEntity.ok(stockAnalyticsService.getCatalogQuality());
    }

}
