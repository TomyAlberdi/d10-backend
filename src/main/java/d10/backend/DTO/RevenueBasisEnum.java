package d10.backend.DTO;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import d10.backend.Model.Invoice;

/**
 * Single source of truth for "which invoices count as revenue".
 *
 * Before this enum existed every reporting method picked its own status set:
 * the yearly summary counted PAGO + ENTREGADO, the best selling products
 * counted PAGO + ENVIADO + ENTREGADO and the category rankings excluded only
 * CANCELADO + DEUDA, which silently counted unaccepted quotes (PENDIENTE) as
 * sales. Three charts on the same screen disagreed about the same month.
 *
 * CANCELADO is never revenue under any basis: the sale did not happen and its
 * stock went back to the shelf.
 */
public enum RevenueBasisEnum {

    /**
     * Money actually collected. Leaves out DEUDA, which is a delivered sale
     * that has not been paid in full. This is the default and the closest
     * match to what the reports did before.
     */
    COLLECTED(EnumSet.of(
            Invoice.Status.PAGO,
            Invoice.Status.ENVIADO,
            Invoice.Status.ENTREGADO)),

    /**
     * Everything that left the building, paid or not. Adds DEUDA, so the
     * revenue figure matches the stock that actually moved.
     */
    DELIVERED(EnumSet.of(
            Invoice.Status.PAGO,
            Invoice.Status.ENVIADO,
            Invoice.Status.ENTREGADO,
            Invoice.Status.DEUDA)),

    /**
     * Commercial pipeline: adds PENDIENTE, i.e. quotes that have not been
     * accepted yet. Useful for demand analysis, never for income reporting.
     */
    QUOTED(EnumSet.of(
            Invoice.Status.PENDIENTE,
            Invoice.Status.PAGO,
            Invoice.Status.ENVIADO,
            Invoice.Status.ENTREGADO,
            Invoice.Status.DEUDA));

    private final Set<Invoice.Status> statuses;

    RevenueBasisEnum(Set<Invoice.Status> statuses) {
        this.statuses = statuses;
    }

    public Set<Invoice.Status> getStatuses() {
        return statuses;
    }

    /**
     * Status names as stored in MongoDB. Aggregation pipelines are built
     * without a typed context, so they need the raw string values rather than
     * the enum constants.
     */
    public List<String> getStatusNames() {
        return statuses.stream().map(Enum::name).toList();
    }

}
