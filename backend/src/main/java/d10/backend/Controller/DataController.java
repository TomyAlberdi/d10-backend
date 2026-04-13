package d10.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
     * Get the 15 best selling products for a given time span and sort criteria
     */
    @GetMapping("/best-selling-products/{timeSpan}/{sortBy}")
    public ResponseEntity<?> getBestSellingProducts(@PathVariable TimeSpanEnum timeSpan, @PathVariable SortByEnum sortBy) {
        return ResponseEntity.ok(dataService.getBestSellingProducts(timeSpan, sortBy));
    }

    /**
     * Get top 5 best selling products by category
     */
    @GetMapping("/top-by-category")
    public ResponseEntity<?> getTop5ByCategory(
            @RequestParam(value = "category") String category,
            @RequestParam(value = "sortBy") SortByEnum sortBy,
            @RequestParam(value = "timespan") TimeSpanEnum timespan) {
        return ResponseEntity.ok(dataService.getTop5ByCategory(category, sortBy, timespan));
    }

    /**
     * Get top 5 best selling products by subcategory
     */
    @GetMapping("/top-by-subcategory")
    public ResponseEntity<?> getTop5BySubcategory(
            @RequestParam(value = "subcategory") String subcategory,
            @RequestParam(value = "sortBy") SortByEnum sortBy,
            @RequestParam(value = "timespan") TimeSpanEnum timespan) {
        return ResponseEntity.ok(dataService.getTop5BySubcategory(subcategory, sortBy, timespan));
    }
}
