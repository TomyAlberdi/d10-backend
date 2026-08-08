package d10.backend.DTO.Order;

import java.time.LocalDate;
import java.time.LocalDateTime;

import d10.backend.Model.Product;
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

    private String productId;

    private String productCode;

    private String productName;

    private String providerName;

    private Integer saleUnitQuantity;

    private Product.SaleType saleUnitType;

    private LocalDate orderDate;

    private Boolean received;

    private LocalDateTime receivedDatetime;

    private String detail;

}
