package d10.backend.DTO.Order;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Payload to create or update a restock order. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDTO {

    /** Optional, defaults to the current date. */
    private LocalDate date;

    private List<CreateOrderProductDTO> products;

}
