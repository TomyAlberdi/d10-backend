package d10.backend.DTO.Data;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeadStockItemDTO {
    private String productId;
    private String code;
    private String name;
    private String category;
    private String providerName;
    private Integer unitsOnHand;
    private Double costValue;
    /** Null when the product has never left stock. */
    private LocalDateTime lastMovement;
    /** Null when the product has never left stock. */
    private Integer daysIdle;
}
