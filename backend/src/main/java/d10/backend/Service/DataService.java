package d10.backend.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import d10.backend.DTO.Invoice.MonthlySummaryRecordDTO;
import d10.backend.DTO.Product.BestSellingProductDTO;
import d10.backend.DTO.SortByEnum;
import d10.backend.DTO.TimeSpanEnum;
import d10.backend.Model.Invoice;
import d10.backend.Model.InvoiceProduct;
import d10.backend.Model.Product;
import d10.backend.Repository.InvoiceRepository;
import d10.backend.Repository.ProductRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DataService {

    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;

    /**
     * Get yearly sales data with monthly summaries
     * @param year the year to get sales data for
     * @return list of monthly summaries with income = 0 for months without invoices
     */
    public List<MonthlySummaryRecordDTO> getYearlySalesData(Integer year) {
        // Create date range for the entire year
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31).plusDays(1);

        // Statuses to include in the query
        List<Invoice.Status> statuses = List.of(
            Invoice.Status.PAGO,
            Invoice.Status.ENVIADO,
            Invoice.Status.ENTREGADO
        );

        // Get invoices for the year with specified statuses
        List<Invoice> invoices = invoiceRepository.findByDateRangeAndStatus(startDate, endDate, statuses);

        // Initialize result with all months set to 0 income
        List<MonthlySummaryRecordDTO> monthlySummaries = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            monthlySummaries.add(new MonthlySummaryRecordDTO(
                month,
                year,
                BigDecimal.ZERO
            ));
        }

        // Calculate monthly totals from invoices
        for (Invoice invoice : invoices) {
            int month = invoice.getDate().getMonthValue();
            Double totalValue = invoice.getTotal();
            if (totalValue == null) totalValue = 0.0;
            BigDecimal invoiceTotal = BigDecimal.valueOf(totalValue);

            MonthlySummaryRecordDTO monthlySummary = monthlySummaries.get(month - 1);
            monthlySummary.setIncome(monthlySummary.getIncome().add(invoiceTotal));
        }

        return monthlySummaries;
    }

    /**
     * Get data analysis results from Product model
     */
    public Object getProductAnalysis() {
        // TODO: Implement product data analysis
        return null;
    }

    /**
     * Get data analysis results from Client model
     */
    public Object getClientAnalysis() {
        // TODO: Implement client data analysis
        return null;
    }

    /**
     * Get data analysis results from Invoice model
     */
    public Object getInvoiceAnalysis() {
        // TODO: Implement invoice data analysis
        return null;
    }

    /**
     * Get data analysis results from CashRegister model
     */
    public Object getCashRegisterAnalysis() {
        // TODO: Implement cash register data analysis
        return null;
    }

    /**
     * Get data analysis results from Warehouse model
     */
    public Object getWarehouseAnalysis() {
        // TODO: Implement warehouse data analysis
        return null;
    }

    /**
     * Get data analysis results from Provider model
     */
    public Object getProviderAnalysis() {
        // TODO: Implement provider data analysis
        return null;
    }

    /**
     * Get comprehensive data analysis from all models
     */
    public Object getComprehensiveAnalysis() {
        // TODO: Implement comprehensive data analysis
        return null;
    }

    /**
     * Get the 15 best selling products for a given time span and sort criteria
     * @param timeSpan the time span to filter invoices (LAST_MONTH, LAST_YEAR, or ALL_TIME)
     * @param sortBy the field to sort by (SALES_AMOUNT, GROSS_INCOME, or NET_INCOME)
     * @return list of up to 15 best selling products with sales metrics
     */
    public List<BestSellingProductDTO> getBestSellingProducts(TimeSpanEnum timeSpan, SortByEnum sortBy) {
        // Determine date range based on timeSpan
        LocalDate startDate = calculateStartDate(timeSpan);
        LocalDate endDate = LocalDate.now().plusDays(1);

        // Statuses to include in the query
        List<Invoice.Status> statuses = List.of(
            Invoice.Status.PAGO,
            Invoice.Status.ENVIADO,
            Invoice.Status.ENTREGADO
        );

        // Get invoices for the time span with specified statuses
        List<Invoice> invoices = invoiceRepository.findByDateRangeAndStatus(startDate, endDate, statuses);

        // Create a map to aggregate product sales data
        Map<String, ProductSalesData> productSalesMap = new HashMap<>();

        for (Invoice invoice : invoices) {
            List<InvoiceProduct> products = invoice.getProducts();
            if (products != null) {
                for (InvoiceProduct invoiceProduct : products) {
                    String productId = invoiceProduct.getId();

                    ProductSalesData salesData = productSalesMap.getOrDefault(productId, 
                        new ProductSalesData());

                    // Increment sales amount
                    salesData.salesAmount++;

                    // Add to total surface
                    double measureUnitQuantity = 0.0;
                    if (invoiceProduct.getMeasureUnitQuantity() != null) {
                        measureUnitQuantity = invoiceProduct.getMeasureUnitQuantity();
                    }
                    salesData.totalSurface += measureUnitQuantity;

                    // Add to total income
                    double subtotal = 0.0;
                    if (invoiceProduct.getSubtotal() != null) {
                        subtotal = invoiceProduct.getSubtotal();
                    }
                    salesData.totalIncome += subtotal;

                    // Add to total sale units for net income calculation
                    int saleUnitQuantity = 0;
                    if (invoiceProduct.getSaleUnitQuantity() != null) {
                        saleUnitQuantity = invoiceProduct.getSaleUnitQuantity();
                    }
                    salesData.totalSaleUnits += saleUnitQuantity;

                    productSalesMap.put(productId, salesData);
                }
            }
        }

        // Convert to BestSellingProductDTO and sort by the specified field
        List<BestSellingProductDTO> bestSellingProducts = productSalesMap.entrySet().stream()
            .map(entry -> {
                String productId = entry.getKey();
                ProductSalesData salesData = entry.getValue();

                // Fetch the product from repository
                Product product = productRepository.findById(productId).orElse(null);

                if (product == null) {
                    return null;
                }

                // Calculate net income if cost and profit are available
                Double netIncome = null;
                if (product.getCostBySaleUnit() != null && product.getCostBySaleUnit() > 0 &&
                    product.getProfit() != null && product.getProfit() > 0) {
                    
                    // Total cost = costBySaleUnit * total quantity of sales units sold
                    Double totalCost = salesData.totalSaleUnits * product.getCostBySaleUnit();
                    netIncome = salesData.totalIncome - totalCost;
                }

                BestSellingProductDTO dto = new BestSellingProductDTO();
                dto.setProduct(product);
                dto.setSalesAmount(salesData.salesAmount);
                dto.setTotalSurface(salesData.totalSurface);
                dto.setTotalIncome(salesData.totalIncome);
                dto.setNetIncome(netIncome);

                return dto;
            })
            .filter(dto -> dto != null)
            .sorted((a, b) -> compareBestSellingProducts(a, b, sortBy))
            .limit(15)
            .collect(Collectors.toList());

        return bestSellingProducts;
    }

    /**
     * Calculate the start date based on the time span
     */
    private LocalDate calculateStartDate(TimeSpanEnum timeSpan) {
        LocalDate now = LocalDate.now();
        return switch (timeSpan) {
            case LAST_MONTH -> now.minusMonths(1);
            case LAST_YEAR -> now.minusYears(1);
            case ALL_TIME -> LocalDate.of(1900, 1, 1); // Arbitrary old date
        };
    }

    /**
     * Compare two BestSellingProductDTO objects based on the sort criteria
     */
    private int compareBestSellingProducts(BestSellingProductDTO a, BestSellingProductDTO b, SortByEnum sortBy) {
        return switch (sortBy) {
            case SALES_AMOUNT -> b.getSalesAmount().compareTo(a.getSalesAmount());
            case GROSS_INCOME -> {
                Double aIncome = a.getTotalIncome();
                Double bIncome = b.getTotalIncome();
                if (aIncome == null) aIncome = 0.0;
                if (bIncome == null) bIncome = 0.0;
                yield bIncome.compareTo(aIncome);
            }
            case NET_INCOME -> {
                Double aNetIncome = a.getNetIncome();
                Double bNetIncome = b.getNetIncome();
                if (aNetIncome == null) aNetIncome = 0.0;
                if (bNetIncome == null) bNetIncome = 0.0;
                yield bNetIncome.compareTo(aNetIncome);
            }
        };
    }

    /**
     * Helper class to aggregate product sales data
     */
    private static class ProductSalesData {
        int salesAmount = 0;
        double totalSurface = 0.0;
        double totalIncome = 0.0;
        double totalSaleUnits = 0.0;
    }
}
