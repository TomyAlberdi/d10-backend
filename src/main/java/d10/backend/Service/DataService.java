package d10.backend.Service;

import static d10.backend.Service.AnalyticsSupport.INVOICES;
import static d10.backend.Service.AnalyticsSupport.TRANSACTIONS;
import static d10.backend.Service.AnalyticsSupport.asDouble;
import static d10.backend.Service.AnalyticsSupport.asInt;
import static d10.backend.Service.AnalyticsSupport.asLocalDate;
import static d10.backend.Service.AnalyticsSupport.asString;
import static d10.backend.Service.AnalyticsSupport.round2;
import static d10.backend.Service.AnalyticsSupport.share;
import static d10.backend.Service.AnalyticsSupport.toDate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import d10.backend.DTO.Data.AvailableYearDTO;
import d10.backend.DTO.Data.CategoryRevenueDTO;
import d10.backend.DTO.Data.DebtorDTO;
import d10.backend.DTO.Data.KpiSummaryDTO;
import d10.backend.DTO.Data.MonthlyCashFlowDTO;
import d10.backend.DTO.Data.MonthlySalesRecordDTO;
import d10.backend.DTO.Data.PaymentMethodRecordDTO;
import d10.backend.DTO.Data.ProviderPerformanceDTO;
import d10.backend.DTO.Data.ReceivableBucketDTO;
import d10.backend.DTO.Data.StockValuationDTO;
import d10.backend.DTO.Data.TopClientDTO;
import d10.backend.DTO.Invoice.MonthlySummaryRecordDTO;
import d10.backend.DTO.Product.BestSellingProductDTO;
import d10.backend.DTO.Product.TopSellingProductDTO;
import d10.backend.DTO.RevenueBasisEnum;
import d10.backend.DTO.SortByEnum;
import d10.backend.DTO.TimeSpanEnum;
import d10.backend.Model.CashRegister;
import d10.backend.Model.Client;
import d10.backend.Model.Invoice;
import d10.backend.Model.Product;
import d10.backend.Repository.ProductRepository;
import lombok.AllArgsConstructor;

/**
 * Read side of the application: every method here answers one chart.
 *
 * All the heavy grouping runs as a MongoDB aggregation pipeline. The previous
 * implementation pulled whole collections into memory with findAll() and then
 * called productRepository.findById() inside the line item loop, so a single
 * category ranking cost one round trip per line of every invoice ever issued.
 * What is left in Java is only the work the pipeline cannot reach: the product
 * documents themselves, fetched once with findAllById.
 *
 * Field names are written as they are stored, not as they are declared in the
 * model: the id of an embedded object is persisted as _id, so invoice lines
 * are matched on products._id and clients on client._id.
 *
 * Endpoint contract: monthly series take a year, everything else takes an
 * optional from/to range.
 */
@Service
@AllArgsConstructor
public class DataService {

    private final ProductRepository productRepository;
    private final StockAnalyticsService stockAnalyticsService;
    private final MongoTemplate mongoTemplate;

    /** Surface below which a product counts as fully cost-snapshotted. */
    private static final double SURFACE_TOLERANCE = 0.0001;

    /** Shown instead of an empty label when a product has no category or provider. */
    private static final String UNCLASSIFIED = "Sin clasificar";

    /** Payment method bucket for invoices issued before the field existed. */
    private static final String UNSPECIFIED_METHOD = "UNSPECIFIED";

    /** Upper bound of each receivables age bucket, in days. */
    private static final int[] AGING_BUCKET_LIMITS = { 30, 60, 90 };
    private static final String[] AGING_BUCKET_LABELS = { "0-30", "31-60", "61-90", "90+" };

    // =====================================================================
    // Sales
    // =====================================================================

    /**
     * Monthly income for a year, with income = 0 for months without sales.
     */
    public List<MonthlySummaryRecordDTO> getYearlySalesData(Integer year, RevenueBasisEnum basis) {
        Map<Integer, Document> byMonth = monthlyInvoiceTotals(year, basis);

        List<MonthlySummaryRecordDTO> monthlySummaries = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            Document row = byMonth.get(month);
            double income = row == null ? 0.0 : asDouble(row.get("income"));
            monthlySummaries.add(new MonthlySummaryRecordDTO(month, year, BigDecimal.valueOf(round2(income))));
        }
        return monthlySummaries;
    }

    /**
     * A1 - Monthly sales for several years at once, so a year can be read
     * against the one before it. Building materials are strongly seasonal and
     * a single year hides the shape.
     */
    public List<MonthlySalesRecordDTO> getMonthlySales(List<Integer> years, RevenueBasisEnum basis) {
        List<Integer> requested = years == null || years.isEmpty()
                ? List.of(LocalDate.now().getYear())
                : years;

        List<MonthlySalesRecordDTO> records = new ArrayList<>();
        for (Integer year : requested.stream().distinct().sorted().toList()) {
            Map<Integer, Document> byMonth = monthlyInvoiceTotals(year, basis);
            for (int month = 1; month <= 12; month++) {
                Document row = byMonth.get(month);
                double income = row == null ? 0.0 : asDouble(row.get("income"));
                int invoiceCount = row == null ? 0 : asInt(row.get("invoiceCount"));
                records.add(new MonthlySalesRecordDTO(
                        year,
                        month,
                        round2(income),
                        invoiceCount,
                        row == null ? 0 : asInt(row.get("unitsSold")),
                        row == null ? 0.0 : round2(asDouble(row.get("surfaceSold"))),
                        invoiceCount == 0 ? 0.0 : round2(income / invoiceCount)));
            }
        }
        return records;
    }

    /**
     * Years that actually contain invoices, most recent first.
     */
    public List<AvailableYearDTO> getAvailableYears() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("date").ne(null)),
                Aggregation.project().and(DateOperators.Year.yearOf("date")).as("year"),
                Aggregation.group("year").count().as("invoiceCount"),
                Aggregation.sort(Sort.Direction.DESC, "_id"));

        return run(aggregation, INVOICES).stream()
                .map(row -> new AvailableYearDTO(asInt(row.get("_id")), asInt(row.get("invoiceCount"))))
                .toList();
    }

    /**
     * A5 - Revenue split by payment method, month by month.
     *
     * The method was added after the system was already in use, so invoices
     * without one are reported as unspecified rather than folded into cash.
     */
    public List<PaymentMethodRecordDTO> getRevenueByPaymentMethod(Integer year, RevenueBasisEnum basis) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year + 1, 1, 1);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(revenueCriteria(startDate, endDate, basis)),
                Aggregation.project()
                        .and(DateOperators.Month.monthOf("date")).as("month")
                        .and(ConditionalOperators.ifNull("paymentMethod").then(UNSPECIFIED_METHOD)).as("method")
                        .and(ConditionalOperators.ifNull("total").then(0.0)).as("total"),
                Aggregation.group("month", "method").sum("total").as("amount"));

        Map<Integer, Map<String, Double>> byMonth = new HashMap<>();
        for (Document row : run(aggregation, INVOICES)) {
            Document key = (Document) row.get("_id");
            if (key == null) {
                continue;
            }
            byMonth.computeIfAbsent(asInt(key.get("month")), month -> new HashMap<>())
                    .merge(String.valueOf(key.get("method")), asDouble(row.get("amount")), Double::sum);
        }

        List<PaymentMethodRecordDTO> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            Map<String, Double> amounts = byMonth.getOrDefault(month, Map.of());
            double cash = amounts.getOrDefault(Invoice.PaymentMethod.CASH.name(), 0.0);
            double digital = amounts.getOrDefault(Invoice.PaymentMethod.DIGITAL.name(), 0.0);
            double usd = amounts.getOrDefault(Invoice.PaymentMethod.USD.name(), 0.0);
            double unspecified = amounts.getOrDefault(UNSPECIFIED_METHOD, 0.0);
            months.add(new PaymentMethodRecordDTO(
                    year, month,
                    round2(cash), round2(digital), round2(usd), round2(unspecified),
                    round2(cash + digital + usd + unspecified)));
        }
        return months;
    }

    // =====================================================================
    // A0 - KPI summary
    // =====================================================================

    /**
     * The headline figures of a period, each next to the same figure over the
     * period of equal length immediately before it.
     *
     * Outstanding debt and stock value are deliberately point in time, not
     * period totals: both answer "how much is tied up right now".
     */
    public KpiSummaryDTO getKpiSummary(LocalDate from, LocalDate to, RevenueBasisEnum basis) {
        LocalDate start = from != null ? from : LocalDate.now().withDayOfYear(1);
        LocalDate end = to != null ? to : LocalDate.now().plusDays(1);

        long periodDays = Math.max(1, ChronoUnit.DAYS.between(start, end));
        LocalDate previousStart = start.minusDays(periodDays);

        Document current = invoiceTotals(start, end, basis);
        Document previous = invoiceTotals(previousStart, start, basis);

        double revenue = asDouble(current.get("income"));
        int invoiceCount = asInt(current.get("invoiceCount"));
        double revenuePrevious = asDouble(previous.get("income"));
        int invoiceCountPrevious = asInt(previous.get("invoiceCount"));

        List<ReceivableBucketDTO> aging = getReceivablesAging();
        double outstandingDebt = aging.stream().mapToDouble(ReceivableBucketDTO::getOutstanding).sum();
        int outstandingInvoices = aging.stream().mapToInt(ReceivableBucketDTO::getInvoiceCount).sum();

        return new KpiSummaryDTO(
                round2(revenue),
                round2(revenuePrevious),
                invoiceCount == 0 ? 0.0 : round2(revenue / invoiceCount),
                invoiceCountPrevious == 0 ? 0.0 : round2(revenuePrevious / invoiceCountPrevious),
                invoiceCount,
                invoiceCountPrevious,
                round2(outstandingDebt),
                outstandingInvoices,
                round2(stockAnalyticsService.getTotalStockValueAtCost()),
                round2(netCash(start, end)));
    }

    // =====================================================================
    // Product rankings
    // =====================================================================

    /**
     * The 15 best selling products for a time span and sort criteria.
     */
    public List<BestSellingProductDTO> getBestSellingProducts(TimeSpanEnum timeSpan, SortByEnum sortBy,
            RevenueBasisEnum basis) {
        return buildProductSales(calculateStartDate(timeSpan), LocalDate.now().plusDays(1), basis).stream()
                .sorted(comparator(sortBy))
                .limit(15)
                .toList();
    }

    /**
     * The 5 best selling products of a category.
     */
    public List<TopSellingProductDTO> getTop5ByCategory(String category, SortByEnum sortBy, TimeSpanEnum timespan,
            RevenueBasisEnum basis) {
        return top5(idsOf(productRepository.findByCategoryIgnoreCase(category)), sortBy, timespan, basis);
    }

    /**
     * The 5 best selling products of a subcategory.
     */
    public List<TopSellingProductDTO> getTop5BySubcategory(String subcategory, SortByEnum sortBy, TimeSpanEnum timespan,
            RevenueBasisEnum basis) {
        return top5(idsOf(productRepository.findBySubcategoryIgnoreCase(subcategory)), sortBy, timespan, basis);
    }

    private List<TopSellingProductDTO> top5(Set<String> productIds, SortByEnum sortBy, TimeSpanEnum timespan,
            RevenueBasisEnum basis) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        return buildProductSales(calculateStartDate(timespan), LocalDate.now().plusDays(1), basis).stream()
                .filter(dto -> productIds.contains(dto.getProduct().getId()))
                .sorted(comparator(sortBy))
                .limit(5)
                .map(dto -> new TopSellingProductDTO(
                        dto.getProduct(),
                        dto.getInvoiceCount(),
                        dto.getUnitsSold(),
                        dto.getTotalIncome(),
                        dto.getNetIncome(),
                        dto.getCostBasisEstimated(),
                        timespan))
                .toList();
    }

    // =====================================================================
    // A3 - Revenue mix
    // =====================================================================

    /** Whether a revenue mix is grouped by category or by subcategory. */
    public enum CategoryLevelEnum {
        CATEGORY, SUBCATEGORY
    }

    /**
     * A3 - How revenue splits across the catalog.
     *
     * Category lives on the product, not on the invoice line, so the sales are
     * aggregated per product first and grouped afterwards against the product
     * documents already fetched for the ranking.
     */
    public List<CategoryRevenueDTO> getRevenueByCategory(LocalDate from, LocalDate to, CategoryLevelEnum level,
            RevenueBasisEnum basis) {
        List<BestSellingProductDTO> sales = buildProductSales(from, to, basis);

        Map<String, CategoryRevenueDTO> byKey = new LinkedHashMap<>();
        for (BestSellingProductDTO sale : sales) {
            String key = level == CategoryLevelEnum.SUBCATEGORY
                    ? sale.getProduct().getSubcategory()
                    : sale.getProduct().getCategory();
            if (key == null || key.isBlank()) {
                key = UNCLASSIFIED;
            }
            CategoryRevenueDTO entry = byKey.computeIfAbsent(key,
                    name -> new CategoryRevenueDTO(name, 0.0, 0, 0, 0.0));
            entry.setRevenue(entry.getRevenue() + sale.getTotalIncome());
            entry.setUnitsSold(entry.getUnitsSold() + sale.getUnitsSold());
            entry.setSkuCount(entry.getSkuCount() + 1);
        }

        double total = byKey.values().stream().mapToDouble(CategoryRevenueDTO::getRevenue).sum();
        List<CategoryRevenueDTO> mix = new ArrayList<>(byKey.values());
        for (CategoryRevenueDTO entry : mix) {
            entry.setSharePct(round2(share(entry.getRevenue(), total)));
            entry.setRevenue(round2(entry.getRevenue()));
        }
        mix.sort(Comparator.comparingDouble(CategoryRevenueDTO::getRevenue).reversed());
        return mix;
    }

    // =====================================================================
    // E1 - Providers
    // =====================================================================

    /**
     * E1 - Revenue, margin and immobilised stock per supplier.
     *
     * Joins on the provider name copied onto each product, which is how the
     * rest of the system already relates the two.
     */
    public List<ProviderPerformanceDTO> getProviderPerformance(LocalDate from, LocalDate to, RevenueBasisEnum basis) {
        List<BestSellingProductDTO> sales = buildProductSales(from, to, basis);

        Map<String, Double> stockByProvider = new HashMap<>();
        for (StockValuationDTO valuation : stockAnalyticsService
                .getStockValuation(StockAnalyticsService.StockGroupByEnum.PROVIDER)) {
            stockByProvider.put(valuation.getKey(), valuation.getCostValue());
        }

        Map<String, ProviderPerformanceDTO> byProvider = new LinkedHashMap<>();
        Map<String, Boolean> costKnown = new HashMap<>();

        for (BestSellingProductDTO sale : sales) {
            String provider = sale.getProduct().getProviderName();
            if (provider == null || provider.isBlank()) {
                provider = UNCLASSIFIED;
            }
            ProviderPerformanceDTO entry = byProvider.computeIfAbsent(provider,
                    name -> new ProviderPerformanceDTO(name, 0.0, 0.0, 0.0, 0.0, 0, 0,
                            stockByProvider.getOrDefault(name, 0.0), false));

            entry.setRevenue(entry.getRevenue() + sale.getTotalIncome());
            entry.setUnitsSold(entry.getUnitsSold() + sale.getUnitsSold());
            entry.setSkuCount(entry.getSkuCount() + 1);
            if (Boolean.TRUE.equals(sale.getCostBasisEstimated())) {
                entry.setCostBasisEstimated(true);
            }
            // A product with no known cost makes the whole supplier margin
            // meaningless, so it is tracked rather than silently counted as 0.
            if (sale.getNetIncome() == null) {
                costKnown.put(provider, false);
            } else {
                costKnown.putIfAbsent(provider, true);
                entry.setCost(entry.getCost() + (sale.getTotalIncome() - sale.getNetIncome()));
            }
        }

        List<ProviderPerformanceDTO> providers = new ArrayList<>(byProvider.values());
        for (ProviderPerformanceDTO entry : providers) {
            boolean known = Boolean.TRUE.equals(costKnown.get(entry.getProviderName()));
            entry.setRevenue(round2(entry.getRevenue()));
            if (known) {
                entry.setCost(round2(entry.getCost()));
                entry.setMargin(round2(entry.getRevenue() - entry.getCost()));
                entry.setMarginPct(round2(share(entry.getRevenue() - entry.getCost(), entry.getRevenue())));
            } else {
                entry.setCost(null);
                entry.setMargin(null);
                entry.setMarginPct(null);
            }
        }
        providers.sort(Comparator.comparingDouble(ProviderPerformanceDTO::getRevenue).reversed());
        return providers;
    }

    // =====================================================================
    // B1 / B2 - Receivables
    // =====================================================================

    /**
     * B1 - Outstanding balance on delivered but unpaid sales, by age.
     *
     * DEUDA is stamped automatically whenever stock has left and the payment
     * does not cover the total, so it is the whole of the receivables book.
     * Every other report leaves it out, which is why this money was invisible.
     */
    public List<ReceivableBucketDTO> getReceivablesAging() {
        LocalDate today = LocalDate.now();

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").is(Invoice.Status.DEUDA.name())),
                Aggregation.project()
                        .and("date").as("date")
                        .and(ConditionalOperators.ifNull("total").then(0.0)).as("total")
                        .and(ConditionalOperators.ifNull("partialPayment").then(0.0)).as("partialPayment"));

        double[] outstandingByBucket = new double[AGING_BUCKET_LABELS.length];
        int[] countByBucket = new int[AGING_BUCKET_LABELS.length];
        int[] oldestByBucket = new int[AGING_BUCKET_LABELS.length];

        for (Document row : run(aggregation, INVOICES)) {
            double outstanding = asDouble(row.get("total")) - asDouble(row.get("partialPayment"));
            if (outstanding <= 0) {
                continue;
            }
            LocalDate date = asLocalDate(row.get("date"));
            int days = date == null ? 0 : (int) ChronoUnit.DAYS.between(date, today);
            int bucket = bucketOf(days);

            outstandingByBucket[bucket] += outstanding;
            countByBucket[bucket]++;
            oldestByBucket[bucket] = Math.max(oldestByBucket[bucket], days);
        }

        List<ReceivableBucketDTO> buckets = new ArrayList<>();
        for (int i = 0; i < AGING_BUCKET_LABELS.length; i++) {
            buckets.add(new ReceivableBucketDTO(
                    AGING_BUCKET_LABELS[i],
                    countByBucket[i],
                    round2(outstandingByBucket[i]),
                    oldestByBucket[i]));
        }
        return buckets;
    }

    /**
     * B2 - Who owes the most, oldest debt first when amounts are close.
     */
    public List<DebtorDTO> getTopDebtors(int limit) {
        LocalDate today = LocalDate.now();

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").is(Invoice.Status.DEUDA.name()).and("client").ne(null)),
                Aggregation.project()
                        .and("client._id").as("clientId")
                        .and("client.name").as("clientName")
                        .and("date").as("date")
                        .and(ConditionalOperators.ifNull("total").then(0.0)).as("total")
                        .and(ConditionalOperators.ifNull("partialPayment").then(0.0)).as("partialPayment"),
                Aggregation.group("clientId")
                        .last("clientName").as("clientName")
                        .sum("total").as("totalInvoiced")
                        .sum("partialPayment").as("paid")
                        .count().as("invoiceCount")
                        .min("date").as("oldestInvoice"));

        List<DebtorDTO> debtors = new ArrayList<>();
        for (Document row : run(aggregation, INVOICES)) {
            double totalInvoiced = asDouble(row.get("totalInvoiced"));
            double paid = asDouble(row.get("paid"));
            double outstanding = totalInvoiced - paid;
            if (outstanding <= 0) {
                continue;
            }
            LocalDate oldest = asLocalDate(row.get("oldestInvoice"));
            debtors.add(new DebtorDTO(
                    asString(row.get("_id")),
                    row.getString("clientName"),
                    round2(outstanding),
                    round2(totalInvoiced),
                    round2(share(paid, totalInvoiced)),
                    asInt(row.get("invoiceCount")),
                    oldest == null ? 0 : (int) ChronoUnit.DAYS.between(oldest, today)));
        }

        debtors.sort(Comparator.comparingDouble(DebtorDTO::getOutstanding).reversed());
        return debtors.size() > limit ? debtors.subList(0, limit) : debtors;
    }

    // =====================================================================
    // Cash register
    // =====================================================================

    /**
     * Monthly cash in, cash out and net movement for a year.
     *
     * Transactions written before the registers were split carry no
     * registerType, so they are only counted when no type filter is asked for.
     */
    public List<MonthlyCashFlowDTO> getMonthlyCashFlow(Integer year, CashRegister.CashRegisterType registerType) {
        LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(year + 1, 1, 1).atStartOfDay();

        Criteria criteria = Criteria.where("dateTime").gte(toDate(start)).lt(toDate(end));
        if (registerType != null) {
            criteria = criteria.and("registerType").is(registerType.name());
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.project()
                        .and(DateOperators.Month.monthOf("dateTime")).as("month")
                        .and(ConditionalOperators.when(Criteria.where("type").is("IN"))
                                .thenValueOf("amount").otherwise(0)).as("inAmount")
                        .and(ConditionalOperators.when(Criteria.where("type").is("OUT"))
                                .thenValueOf("amount").otherwise(0)).as("outAmount"),
                Aggregation.group("month")
                        .sum("inAmount").as("inTotal")
                        .sum("outAmount").as("outTotal"));

        Map<Integer, Document> byMonth = new HashMap<>();
        for (Document row : run(aggregation, TRANSACTIONS)) {
            byMonth.put(asInt(row.get("_id")), row);
        }

        List<MonthlyCashFlowDTO> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            Document row = byMonth.get(month);
            double inTotal = row == null ? 0.0 : asDouble(row.get("inTotal"));
            double outTotal = row == null ? 0.0 : asDouble(row.get("outTotal"));
            months.add(new MonthlyCashFlowDTO(month, year, round2(inTotal), round2(outTotal),
                    round2(inTotal - outTotal)));
        }
        return months;
    }

    // =====================================================================
    // Clients
    // =====================================================================

    /**
     * Clients ranked by revenue over a period.
     *
     * Grouping runs on the client snapshot embedded in each invoice, so a
     * client renamed after the sale still reports under the name that was on
     * the invoice. Invoices with no client attached are left out.
     */
    public List<TopClientDTO> getTopClients(LocalDate from, LocalDate to, int limit, RevenueBasisEnum basis) {
        Criteria criteria = revenueCriteria(from, to, basis).and("client").ne(null);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.project()
                        .and("client._id").as("clientId")
                        .and("client.name").as("clientName")
                        .and("client.type").as("clientType")
                        .and("date").as("date")
                        .and(ConditionalOperators.ifNull("total").then(0.0)).as("total"),
                Aggregation.group("clientId")
                        .sum("total").as("revenue")
                        .count().as("invoiceCount")
                        .last("clientName").as("clientName")
                        .last("clientType").as("clientType")
                        .max("date").as("lastPurchase"),
                Aggregation.sort(Sort.Direction.DESC, "revenue"),
                Aggregation.limit(limit));

        List<TopClientDTO> clients = new ArrayList<>();
        for (Document row : run(aggregation, INVOICES)) {
            double revenue = asDouble(row.get("revenue"));
            int invoiceCount = asInt(row.get("invoiceCount"));
            clients.add(new TopClientDTO(
                    asString(row.get("_id")),
                    row.getString("clientName"),
                    asClientType(row.get("clientType")),
                    round2(revenue),
                    invoiceCount,
                    invoiceCount == 0 ? 0.0 : round2(revenue / invoiceCount),
                    asLocalDate(row.get("lastPurchase"))));
        }
        return clients;
    }

    // =====================================================================
    // Shared pipelines
    // =====================================================================

    /**
     * Aggregates every invoice line of the period into one row per product and
     * joins the product documents in a single findAllById.
     *
     * The result is bounded by the number of products ever sold, not by the
     * number of invoices, so sorting and slicing it in Java is cheap. Sorting
     * could not happen in the pipeline anyway: net income needs the current
     * product cost for the lines that predate the cost snapshot.
     */
    private List<BestSellingProductDTO> buildProductSales(LocalDate from, LocalDate to, RevenueBasisEnum basis) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(revenueCriteria(from, to, basis)),
                Aggregation.unwind("products"),
                Aggregation.project()
                        .and("_id").as("invoiceId")
                        .and("products._id").as("productId")
                        .and(ConditionalOperators.ifNull("products.subtotal").then(0.0)).as("lineIncome")
                        .and(ConditionalOperators.ifNull("products.saleUnitQuantity").then(0)).as("lineUnits")
                        .and(ConditionalOperators.ifNull("products.measureUnitQuantity").then(0.0)).as("lineMeasure")
                        .and(ConditionalOperators.ifNull("products.costByMeasureUnitAtSale").then(0.0))
                        .as("lineCostPerMeasure"),
                Aggregation.project("invoiceId", "productId", "lineIncome", "lineUnits", "lineMeasure")
                        .and(ArithmeticOperators.valueOf("lineCostPerMeasure").multiplyBy("lineMeasure")).as("lineCost")
                        .and(ConditionalOperators.when(Criteria.where("lineCostPerMeasure").gt(0))
                                .thenValueOf("lineMeasure").otherwise(0)).as("lineMeasureWithCost"),
                Aggregation.group("productId")
                        .sum("lineIncome").as("totalIncome")
                        .sum("lineUnits").as("unitsSold")
                        .sum("lineMeasure").as("totalSurface")
                        .sum("lineCost").as("costFromSnapshots")
                        .sum("lineMeasureWithCost").as("surfaceWithCost")
                        .addToSet("invoiceId").as("invoiceIds"),
                Aggregation.project("totalIncome", "unitsSold", "totalSurface", "costFromSnapshots", "surfaceWithCost")
                        .and("_id").as("productId")
                        .and(ArrayOperators.Size.lengthOfArray("invoiceIds")).as("invoiceCount"));

        List<Document> rows = run(aggregation, INVOICES);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<String> productIds = rows.stream()
                .map(row -> asString(row.get("productId")))
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<String, Product> products = new HashMap<>();
        productRepository.findAllById(productIds).forEach(product -> products.put(product.getId(), product));

        List<BestSellingProductDTO> result = new ArrayList<>();
        for (Document row : rows) {
            Product product = products.get(asString(row.get("productId")));
            if (product == null) {
                // Product deleted after the sale: a ranking that renders code,
                // name and measure unit has nothing to show for it.
                continue;
            }

            double totalIncome = asDouble(row.get("totalIncome"));
            double totalSurface = asDouble(row.get("totalSurface"));
            double surfaceWithCost = asDouble(row.get("surfaceWithCost"));
            double costFromSnapshots = asDouble(row.get("costFromSnapshots"));
            double surfaceWithoutCost = Math.max(0.0, totalSurface - surfaceWithCost);
            boolean estimated = surfaceWithoutCost > SURFACE_TOLERANCE;

            Double netIncome = null;
            if (!estimated) {
                netIncome = totalIncome - costFromSnapshots;
            } else {
                Double currentCost = product.getCostByMeasureUnit();
                if (currentCost != null && currentCost > 0) {
                    netIncome = totalIncome - (costFromSnapshots + (surfaceWithoutCost * currentCost));
                }
            }

            BestSellingProductDTO dto = new BestSellingProductDTO();
            dto.setProduct(product);
            dto.setInvoiceCount(asInt(row.get("invoiceCount")));
            dto.setUnitsSold(asInt(row.get("unitsSold")));
            dto.setTotalSurface(round2(totalSurface));
            dto.setTotalIncome(round2(totalIncome));
            dto.setNetIncome(netIncome == null ? null : round2(netIncome));
            dto.setCostBasisEstimated(estimated);
            result.add(dto);
        }
        return result;
    }

    /**
     * Invoice totals of a year, grouped by month.
     *
     * Units and surface are summed straight over the embedded product array,
     * so the pipeline does not have to unwind and then rebuild a distinct
     * invoice count.
     */
    private Map<Integer, Document> monthlyInvoiceTotals(Integer year, RevenueBasisEnum basis) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year + 1, 1, 1);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(revenueCriteria(startDate, endDate, basis)),
                Aggregation.project()
                        .and(DateOperators.Month.monthOf("date")).as("month")
                        .and(ConditionalOperators.ifNull("total").then(0.0)).as("total")
                        .and(AccumulatorOperators.Sum.sumOf("products.saleUnitQuantity")).as("units")
                        .and(AccumulatorOperators.Sum.sumOf("products.measureUnitQuantity")).as("surface"),
                Aggregation.group("month")
                        .sum("total").as("income")
                        .count().as("invoiceCount")
                        .sum("units").as("unitsSold")
                        .sum("surface").as("surfaceSold"));

        Map<Integer, Document> byMonth = new HashMap<>();
        for (Document row : run(aggregation, INVOICES)) {
            byMonth.put(asInt(row.get("_id")), row);
        }
        return byMonth;
    }

    /** Revenue and invoice count of a single period, as one row. */
    private Document invoiceTotals(LocalDate from, LocalDate to, RevenueBasisEnum basis) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(revenueCriteria(from, to, basis)),
                Aggregation.project().and(ConditionalOperators.ifNull("total").then(0.0)).as("total"),
                Aggregation.group().sum("total").as("income").count().as("invoiceCount"));

        List<Document> rows = run(aggregation, INVOICES);
        return rows.isEmpty() ? new Document() : rows.get(0);
    }

    /**
     * Cash in minus cash out over a period. The USD register is excluded: it
     * holds a different currency, the same rule getDailyTotals follows. The
     * $ne also keeps legacy rows that carry no register type at all.
     */
    private double netCash(LocalDate from, LocalDate to) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("dateTime")
                        .gte(toDate(from.atStartOfDay())).lt(toDate(to.atStartOfDay()))
                        .and("registerType").ne(CashRegister.CashRegisterType.USD.name())),
                Aggregation.project()
                        .and(ConditionalOperators.when(Criteria.where("type").is("IN"))
                                .thenValueOf("amount").otherwise(0)).as("inAmount")
                        .and(ConditionalOperators.when(Criteria.where("type").is("OUT"))
                                .thenValueOf("amount").otherwise(0)).as("outAmount"),
                Aggregation.group().sum("inAmount").as("inTotal").sum("outAmount").as("outTotal"));

        List<Document> rows = run(aggregation, TRANSACTIONS);
        if (rows.isEmpty()) {
            return 0.0;
        }
        return asDouble(rows.get(0).get("inTotal")) - asDouble(rows.get(0).get("outTotal"));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /**
     * Date range plus the status set of the requested revenue basis. Every
     * report goes through here, which is what keeps them agreeing with each
     * other.
     */
    private Criteria revenueCriteria(LocalDate startDate, LocalDate endDate, RevenueBasisEnum basis) {
        // Starts on the status, which is always present, so an unbounded range
        // never has to build an empty Criteria with no key.
        Criteria criteria = Criteria.where("status").in(basis.getStatusNames());
        if (startDate != null && endDate != null) {
            criteria = criteria.and("date").gte(toDate(startDate)).lt(toDate(endDate));
        } else if (startDate != null) {
            criteria = criteria.and("date").gte(toDate(startDate));
        } else if (endDate != null) {
            criteria = criteria.and("date").lt(toDate(endDate));
        }
        return criteria;
    }

    private LocalDate calculateStartDate(TimeSpanEnum timeSpan) {
        LocalDate now = LocalDate.now();
        return switch (timeSpan) {
            case THIS_MONTH -> now.withDayOfMonth(1);
            case THIS_YEAR -> now.withDayOfYear(1);
            case ALL_TIME -> LocalDate.of(1900, 1, 1);
        };
    }

    private int bucketOf(int days) {
        for (int i = 0; i < AGING_BUCKET_LIMITS.length; i++) {
            if (days <= AGING_BUCKET_LIMITS[i]) {
                return i;
            }
        }
        return AGING_BUCKET_LABELS.length - 1;
    }

    private Comparator<BestSellingProductDTO> comparator(SortByEnum sortBy) {
        return switch (sortBy) {
            case INVOICE_COUNT -> Comparator.comparingInt(
                    (BestSellingProductDTO dto) -> dto.getInvoiceCount() == null ? 0 : dto.getInvoiceCount())
                    .reversed();
            case UNITS_SOLD -> Comparator.comparingInt(
                    (BestSellingProductDTO dto) -> dto.getUnitsSold() == null ? 0 : dto.getUnitsSold())
                    .reversed();
            case GROSS_INCOME -> Comparator.comparingDouble(
                    (BestSellingProductDTO dto) -> dto.getTotalIncome() == null ? 0.0 : dto.getTotalIncome())
                    .reversed();
            case NET_INCOME -> Comparator.comparingDouble(
                    (BestSellingProductDTO dto) -> dto.getNetIncome() == null ? 0.0 : dto.getNetIncome())
                    .reversed();
        };
    }

    private List<Document> run(Aggregation aggregation, String collection) {
        return mongoTemplate.aggregate(aggregation, collection, Document.class).getMappedResults();
    }

    private Set<String> idsOf(Collection<Product> products) {
        return products.stream().map(Product::getId).collect(Collectors.toSet());
    }

    private static Client.ClientType asClientType(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Client.ClientType.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
