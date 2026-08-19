package d10.backend.DTO;

/**
 * Sort criteria for the product rankings.
 *
 * INVOICE_COUNT and UNITS_SOLD used to share the single name SALES_AMOUNT,
 * which meant "number of invoices containing the product" in one DTO and
 * "sale units sold" in the other. They are different questions and now have
 * different names.
 */
public enum SortByEnum {
    /** How many separate sales included this product. */
    INVOICE_COUNT,
    /** How many sale units (CAJA / JUEGO / UNIDAD) were sold. */
    UNITS_SOLD,
    GROSS_INCOME,
    NET_INCOME
}
