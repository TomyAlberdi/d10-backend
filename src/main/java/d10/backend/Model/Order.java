package d10.backend.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Restock order: a product that has to be bought again, together with the
 * amount of sale units to request. Orders are grouped by {@code orderDate} so
 * the whole batch of a day can be downloaded and received in one go. Receiving
 * an order adds its sale units to the stock of the product.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String productId;
    // Product data is copied on creation so the order keeps reading correctly
    // even if the product is renamed or deleted afterwards.
    private String productCode;
    private String productName;
    private String providerName;
    // Amount to order, expressed in the sale unit of the product (CAJA, JUEGO, UNIDAD)
    private Integer saleUnitQuantity;
    private Product.SaleType saleUnitType;
    private LocalDate orderDate;
    private Boolean received = false;
    private LocalDateTime receivedDatetime;
    // Optional free text, e.g. "Pedir en color blanco"
    private String detail;

}
