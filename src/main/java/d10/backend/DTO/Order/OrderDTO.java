package d10.backend.DTO.Order;

import java.time.LocalDate;
import java.util.List;

import d10.backend.Model.OrderProduct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private String id;

    private LocalDate date;

    private Boolean received;

    private List<OrderProduct> products;

}
