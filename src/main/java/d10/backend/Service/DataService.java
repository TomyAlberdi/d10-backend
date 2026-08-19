package d10.backend.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import d10.backend.DTO.Data.AvailableYearDTO;
import d10.backend.DTO.Data.MonthlyCashFlowDTO;
import d10.backend.DTO.Data.TopClientDTO;
import d10.backend.DTO.Invoice.MonthlySummaryRecordDTO;
import d10.backend.DTO.Product.BestSellingProductDTO;
import d10.backend.DTO.Product.TopSellingProductDTO;
import d10.backend.DTO.RevenueBasisEnum;
import d10.backend.DTO.SortByEnum;
import d10.backend.DTO.TimeSpanEnum;
import d10.backend.Model.CashRegister;
import d10.backend.Model.Client;
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
 */
@Service
@AllArgsConstructor
public class DataService {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    private static final String INVOICES = "invoices";
    private static final String TRANSACTIONS = "cash_register_transactions";

    /** Surface below which a product counts as fully cost-snapshotted. */
    private static final double SURFACE_TOLERANCE = 0.0001;

    // ---------------------------------------------------------------------
    // Sales
    // ---------------------------------------------------------------------

    /**
     * Monthly income for a year, with income = 0 for months without sales.
     *
     * @param year the year to summarise
     * @param basis which invoice statuses count as revenue
     */
    public List<MonthlySummaryRecordDTO> getYearlySalesData(Integer year, RevenueBasisEnum basis) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year + 1, 1, 1);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(revenueCriteria(startDate, endDate, basis)),
                Aggregation.project()
                        .and(DateOperators.Month.monthOf("date")).as("month")
                        .and(ConditionalOperators.ifNull("total").then(0.0)).as("total"),
                Aggregation.group("month").sum("total").as("income"));

        Map<Integer, BigDecimal> incomeByMonth = new HashMap<>();
        for (Document row : run(aggregation, INVOICES)) {
            incomeByMonth.put(asInt(row.get("_id")), BigDecimal.valueOf(asDouble(row.get("income"))));
        }

        List<MonthlySummaryRecordDTO> monthlySummaries = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            monthlySummaries.add(new MonthlySummaryRecordDTO(
                    month,
                    year,
                    incomeByMonth.getOrDefault(month, BigDecimal.ZERO)));
        }
        return monthlySummaries;
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

    // ---------------------------------------------------------------------
    // Product rankings
    // ---------------------------------------------------------------------

    /**
     * The 15 best selling products for a time span and sort criteria.
     */
    public List<BestSellingProductDTO> getBestSellingProducts(TimeSpanEnum timeSpan, SortByEnum sortBy,
            RevenueBasisEnum basis) {
        return buildProductSales(timeSpan, basis).stream()
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
        return buildProductSales(timespan, basis).stream()
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

    /**
     * Aggregates every invoice line of the period into one row per product and
     * joins the product documents in a single findAllById.
     *
     * The result is bounded by the number of products ever sold, not by the
     * number of invoices, so sorting and slicing it in Java is cheap. Sorting
     * could not happen in the pipeline anyway: net income needs the current
     * product cost for the lines that predate the cost snapshot.
     */
    private List<BestSellingProductDTO> buildProductSales(TimeSpanEnum timeSpan, RevenueBasisEnum basis) {
        LocalDate startDate = calculateStartDate(timeSpan);
        LocalDate endDate = LocalDate.now().plusDays(1);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(revenueCriteria(startDate, endDate, basis)),
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
            dto.setTotalSurface(totalSurface);
            dto.setTotalIncome(totalIncome);
            dto.setNetIncome(netIncome);
            dto.setCostBasisEstimated(estimated);
            result.add(dto);
        }
        return result;
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

    // ---------------------------------------------------------------------
    // Cash register
    // ---------------------------------------------------------------------

    /**
     * Monthly cash in, cash out and net movement for a year.
     *
     * Transactions written before the registers were split carry no
     * registerType, so they are only counted when no type filter is asked for.
     *
     * @param registerType optional filter; null totals every register. USD is a
     * different currency and should not be added to the peso registers, the
     * same rule CashRegisterService.getDailyTotals already follows.
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
            months.add(new MonthlyCashFlowDTO(month, year, inTotal, outTotal, inTotal - outTotal));
        }
        return months;
    }

    // ---------------------------------------------------------------------
    // Clients
    // ---------------------------------------------------------------------

    /**
     * Clients ranked by revenue over a period.
     *
     * Grouping runs on the client snapshot embedded in each invoice, so a
     * client renamed after the sale still reports under the name that was on
     * the invoice. Invoices with no client attached are left out.
     *
     * @param from inclusive, null means no lower bound
     * @param to exclusive, null means no upper bound
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
                    revenue,
                    invoiceCount,
                    invoiceCount == 0 ? 0.0 : revenue / invoiceCount,
                    asLocalDate(row.get("lastPurchase"))));
        }
        return clients;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

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

    private List<Document> run(Aggregation aggregation, String collection) {
        return mongoTemplate.aggregate(aggregation, collection, Document.class).getMappedResults();
    }

    private Set<String> idsOf(Collection<Product> products) {
        return products.stream().map(Product::getId).collect(Collectors.toSet());
    }

    /**
     * Spring Data writes a LocalDate as the start of that day in the server
     * zone; matching with the same conversion keeps the range aligned with
     * what is stored.
     */
    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof ObjectId objectId ? objectId.toHexString() : value.toString();
    }

    private static LocalDate asLocalDate(Object value) {
        return value instanceof Date date
                ? date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : null;
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
