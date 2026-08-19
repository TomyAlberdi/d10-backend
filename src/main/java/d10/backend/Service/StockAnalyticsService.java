package d10.backend.Service;

import static d10.backend.Service.AnalyticsSupport.PRODUCTS;
import static d10.backend.Service.AnalyticsSupport.STOCK_LOGS;
import static d10.backend.Service.AnalyticsSupport.asDouble;
import static d10.backend.Service.AnalyticsSupport.asInt;
import static d10.backend.Service.AnalyticsSupport.asLocalDateTime;
import static d10.backend.Service.AnalyticsSupport.asString;
import static d10.backend.Service.AnalyticsSupport.round2;
import static d10.backend.Service.AnalyticsSupport.share;
import static d10.backend.Service.AnalyticsSupport.toDate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import d10.backend.DTO.Data.CatalogQualityDTO;
import d10.backend.DTO.Data.DeadStockDTO;
import d10.backend.DTO.Data.DeadStockItemDTO;
import d10.backend.DTO.Data.StockTurnoverDTO;
import d10.backend.DTO.Data.StockValuationDTO;
import d10.backend.Model.Product;
import d10.backend.Model.StockLog;
import d10.backend.Repository.ProductRepository;
import lombok.AllArgsConstructor;

/**
 * Analytics that read the catalog and the stock movement ledger.
 *
 * Kept apart from {@link DataService}, which answers everything derived from
 * invoices: these questions never touch a sale, and the warehouse is usually
 * the largest asset the business holds.
 */
@Service
@AllArgsConstructor
public class StockAnalyticsService {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    /** Shown instead of an empty label when a product has no category or provider. */
    private static final String UNCLASSIFIED = "Sin clasificar";

    /** How a valuation can be grouped. */
    public enum StockGroupByEnum {
        CATEGORY, SUBCATEGORY, PROVIDER, QUALITY
    }

    // ---------------------------------------------------------------------
    // C1 - Stock valuation
    // ---------------------------------------------------------------------

    /**
     * Stock on hand valued at cost and at list price.
     *
     * Only products actually holding stock are counted, discontinued ones
     * included: capital tied up in a line that is no longer sold is still
     * capital tied up, and arguably the most interesting part of the answer.
     */
    public List<StockValuationDTO> getStockValuation(StockGroupByEnum groupBy) {
        String groupField = switch (groupBy) {
            case CATEGORY -> "category";
            case SUBCATEGORY -> "subcategory";
            case PROVIDER -> "providerName";
            case QUALITY -> "quality";
        };

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("stock.quantity").gt(0)),
                // Split into single multiplications so each stage stays a plain
                // binary expression instead of a nested one.
                Aggregation.project()
                        .and(ConditionalOperators.ifNull(groupField).then(UNCLASSIFIED)).as("key")
                        .and(ConditionalOperators.ifNull("stock.quantity").then(0)).as("quantity")
                        .and(ConditionalOperators.ifNull("costByMeasureUnit").then(0.0)).as("cost")
                        .and(ConditionalOperators.ifNull("measurePerSaleUnit").then(0.0)).as("measurePerSaleUnit")
                        .and(ConditionalOperators.ifNull("priceBySaleUnit").then(0.0)).as("priceBySaleUnit"),
                Aggregation.project("key", "quantity", "priceBySaleUnit")
                        .and(ArithmeticOperators.valueOf("cost").multiplyBy("measurePerSaleUnit")).as("costPerSaleUnit"),
                Aggregation.project("key", "quantity")
                        .and(ArithmeticOperators.valueOf("quantity").multiplyBy("costPerSaleUnit")).as("costValue")
                        .and(ArithmeticOperators.valueOf("quantity").multiplyBy("priceBySaleUnit")).as("retailValue"),
                Aggregation.group("key")
                        .count().as("skuCount")
                        .sum("quantity").as("unitsOnHand")
                        .sum("costValue").as("costValue")
                        .sum("retailValue").as("retailValue"));

        List<Document> rows = mongoTemplate.aggregate(aggregation, PRODUCTS, Document.class).getMappedResults();

        double totalCost = rows.stream().mapToDouble(row -> asDouble(row.get("costValue"))).sum();

        List<StockValuationDTO> valuation = new ArrayList<>();
        for (Document row : rows) {
            double costValue = asDouble(row.get("costValue"));
            double retailValue = asDouble(row.get("retailValue"));
            valuation.add(new StockValuationDTO(
                    asString(row.get("_id")),
                    asInt(row.get("skuCount")),
                    asInt(row.get("unitsOnHand")),
                    round2(costValue),
                    round2(retailValue),
                    round2(retailValue - costValue),
                    round2(share(costValue, totalCost))));
        }
        valuation.sort(Comparator.comparingDouble(StockValuationDTO::getCostValue).reversed());
        return valuation;
    }

    /**
     * Everything on the shelves, at cost, as a single figure for the KPI strip.
     */
    public double getTotalStockValueAtCost() {
        return getStockValuation(StockGroupByEnum.CATEGORY).stream()
                .mapToDouble(StockValuationDTO::getCostValue)
                .sum();
    }

    // ---------------------------------------------------------------------
    // C2 - Turnover
    // ---------------------------------------------------------------------

    /**
     * How many times each product sold through its current stock over the
     * period, next to the capital it holds.
     *
     * Movements come from the stock log rather than from invoices, so manual
     * corrections and returns are reflected too.
     */
    public List<StockTurnoverDTO> getStockTurnover(LocalDate from, LocalDate to, int limit) {
        LocalDate start = from != null ? from : LocalDate.now().minusYears(1);
        LocalDate end = to != null ? to : LocalDate.now().plusDays(1);
        long periodDays = Math.max(1, Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays());

        Map<String, Integer> unitsOutByProduct = unitsOutByProduct(start, end);

        // Only products that hold stock or moved during the period can say
        // anything here, so the catalog is never scanned whole.
        Map<String, Product> candidates = new HashMap<>();
        productRepository.findByStockQuantityGreaterThan(0)
                .forEach(product -> candidates.put(product.getId(), product));
        List<String> soldButOutOfStock = unitsOutByProduct.keySet().stream()
                .filter(id -> !candidates.containsKey(id))
                .toList();
        if (!soldButOutOfStock.isEmpty()) {
            productRepository.findAllById(soldButOutOfStock)
                    .forEach(product -> candidates.put(product.getId(), product));
        }

        List<StockTurnoverDTO> turnover = new ArrayList<>();
        for (Product product : candidates.values()) {
            int unitsOut = unitsOutByProduct.getOrDefault(product.getId(), 0);
            int unitsOnHand = product.getStock() != null && product.getStock().getQuantity() != null
                    ? product.getStock().getQuantity()
                    : 0;

            double dailyRate = (double) unitsOut / periodDays;
            Double turns = unitsOnHand > 0 ? round2((double) unitsOut / unitsOnHand) : null;
            Double daysOfSupply = dailyRate > 0 ? round2(unitsOnHand / dailyRate) : null;

            turnover.add(new StockTurnoverDTO(
                    product.getId(),
                    product.getCode(),
                    product.getName(),
                    product.getCategory(),
                    product.getProviderName(),
                    unitsOut,
                    unitsOnHand,
                    turns,
                    daysOfSupply,
                    round2(costValueOf(product, unitsOnHand))));
        }

        // Biggest capital first: that is where a low turn ratio actually hurts.
        turnover.sort(Comparator.comparingDouble(StockTurnoverDTO::getCostValue).reversed());
        return turnover.size() > limit ? turnover.subList(0, limit) : turnover;
    }

    // ---------------------------------------------------------------------
    // C3 - Dead stock
    // ---------------------------------------------------------------------

    /**
     * Products holding stock that have not moved out in the given number of
     * days, with the capital they immobilise.
     */
    public DeadStockDTO getDeadStock(int daysWithoutSale) {
        LocalDateTime cutoff = LocalDate.now().minusDays(daysWithoutSale).atStartOfDay();
        Map<String, LocalDateTime> lastOutByProduct = lastOutByProduct();

        List<DeadStockItemDTO> items = new ArrayList<>();
        double totalCostValue = 0.0;

        for (Product product : productRepository.findByStockQuantityGreaterThan(0)) {
            LocalDateTime lastMovement = lastOutByProduct.get(product.getId());
            if (lastMovement != null && lastMovement.isAfter(cutoff)) {
                continue;
            }

            int unitsOnHand = product.getStock().getQuantity() != null ? product.getStock().getQuantity() : 0;
            double costValue = costValueOf(product, unitsOnHand);
            totalCostValue += costValue;

            Integer daysIdle = lastMovement == null
                    ? null
                    : (int) Duration.between(lastMovement, LocalDateTime.now()).toDays();

            items.add(new DeadStockItemDTO(
                    product.getId(),
                    product.getCode(),
                    product.getName(),
                    product.getCategory(),
                    product.getProviderName(),
                    unitsOnHand,
                    round2(costValue),
                    lastMovement,
                    daysIdle));
        }

        // Never-moved products first, then the longest idle, then by capital.
        items.sort((left, right) -> {
            Integer leftIdle = left.getDaysIdle();
            Integer rightIdle = right.getDaysIdle();
            if (leftIdle == null && rightIdle != null) {
                return -1;
            }
            if (leftIdle != null && rightIdle == null) {
                return 1;
            }
            if (leftIdle != null && !leftIdle.equals(rightIdle)) {
                return Integer.compare(rightIdle, leftIdle);
            }
            return Double.compare(right.getCostValue(), left.getCostValue());
        });

        return new DeadStockDTO(round2(totalCostValue), items.size(), items);
    }

    // ---------------------------------------------------------------------
    // F3 - Catalog quality
    // ---------------------------------------------------------------------

    /**
     * How much of the catalog is missing the fields the other charts depend
     * on. Products with no cost are the ones worth acting on first: they make
     * every margin and valuation figure quietly wrong.
     */
    public CatalogQualityDTO getCatalogQuality() {
        int total = (int) mongoTemplate.count(new Query(), Product.class);
        int discontinued = count(Criteria.where("discontinued").is(true));

        return new CatalogQualityDTO(
                total,
                total - discontinued,
                discontinued,
                count(Criteria.where("stock.quantity").gt(0)),
                count(missingOrNotPositive("costByMeasureUnit")),
                count(missingOrNotPositive("priceBySaleUnit")),
                count(missingOrBlank("category")),
                count(missingOrBlank("subcategory")),
                count(missingOrEmptyArray("images")),
                count(missingOrEmptyArray("characteristics")),
                count(missingOrBlank("dimensions")),
                count(missingOrBlank("providerName")));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Sale units that left stock per product, over a date range. */
    private Map<String, Integer> unitsOutByProduct(LocalDate start, LocalDate end) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("type").is(StockLog.StockLogType.OUT.name())
                        .and("datetime").gte(toDate(start.atStartOfDay())).lt(toDate(end.atStartOfDay()))),
                Aggregation.project()
                        .and("productId").as("productId")
                        .and(ConditionalOperators.ifNull("saleUnitQuantity").then(0)).as("units"),
                Aggregation.group("productId").sum("units").as("unitsOut"));

        Map<String, Integer> unitsOut = new HashMap<>();
        for (Document row : mongoTemplate.aggregate(aggregation, STOCK_LOGS, Document.class).getMappedResults()) {
            String productId = asString(row.get("_id"));
            if (productId != null) {
                unitsOut.put(productId, asInt(row.get("unitsOut")));
            }
        }
        return unitsOut;
    }

    /** Most recent outbound movement per product, over the whole history. */
    private Map<String, LocalDateTime> lastOutByProduct() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("type").is(StockLog.StockLogType.OUT.name())),
                Aggregation.group("productId").max("datetime").as("lastMovement"));

        Map<String, LocalDateTime> lastOut = new HashMap<>();
        for (Document row : mongoTemplate.aggregate(aggregation, STOCK_LOGS, Document.class).getMappedResults()) {
            String productId = asString(row.get("_id"));
            LocalDateTime lastMovement = asLocalDateTime(row.get("lastMovement"));
            if (productId != null && lastMovement != null) {
                lastOut.put(productId, lastMovement);
            }
        }
        return lastOut;
    }

    /** Cost of a number of sale units, via the cost per measure unit. */
    private double costValueOf(Product product, int units) {
        double cost = product.getCostByMeasureUnit() != null ? product.getCostByMeasureUnit() : 0.0;
        double measurePerSaleUnit = product.getMeasurePerSaleUnit() != null ? product.getMeasurePerSaleUnit() : 0.0;
        return units * cost * measurePerSaleUnit;
    }

    private int count(Criteria criteria) {
        return (int) mongoTemplate.count(Query.query(criteria), Product.class);
    }

    private Criteria missingOrBlank(String field) {
        return new Criteria().orOperator(
                Criteria.where(field).is(null),
                Criteria.where(field).is(""));
    }

    private Criteria missingOrNotPositive(String field) {
        return new Criteria().orOperator(
                Criteria.where(field).is(null),
                Criteria.where(field).lte(0));
    }

    private Criteria missingOrEmptyArray(String field) {
        return new Criteria().orOperator(
                Criteria.where(field).is(null),
                Criteria.where(field).size(0));
    }

}
