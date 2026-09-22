package d10.backend.Model;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A restock order: every product that has to be bought again, written down on a
 * single date. Receiving the order adds the sale units of each line to the
 * stock of its product, and setting it back to pending takes them out again.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private LocalDate date;
    private Boolean received = false;
    private List<OrderProduct> products;

}
