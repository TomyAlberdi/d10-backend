package d10.backend.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * Plumbing shared by the analytics services.
 *
 * Aggregation results are read as raw {@link Document}s rather than mapped
 * onto typed classes: the pipelines project fields that do not exist on any
 * model, and the ids of embedded objects come back as ObjectId. Reading them
 * explicitly keeps the conversion visible instead of relying on the mapper.
 */
final class AnalyticsSupport {

    private AnalyticsSupport() {
    }

    /** Collection names, written once so the pipelines cannot drift apart. */
    static final String INVOICES = "invoices";
    static final String PRODUCTS = "products";
    static final String STOCK_LOGS = "stock_logs";
    static final String TRANSACTIONS = "cash_register_transactions";

    static double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    static String asString(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof ObjectId objectId ? objectId.toHexString() : value.toString();
    }

    static LocalDate asLocalDate(Object value) {
        return value instanceof Date date
                ? date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : null;
    }

    static LocalDateTime asLocalDateTime(Object value) {
        return value instanceof Date date
                ? date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;
    }

    /**
     * Spring Data writes a LocalDate as the start of that day in the server
     * zone; matching with the same conversion keeps ranges aligned with what
     * is stored.
     */
    static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /** Percentage of {@code part} over {@code whole}, 0 when there is no whole. */
    static double share(double part, double whole) {
        return whole == 0.0 ? 0.0 : (part / whole) * 100.0;
    }

    /** Rounds to two decimals so the JSON does not carry float noise. */
    static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

}
