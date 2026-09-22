package d10.backend.DTO.Client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdjustClientBalanceDTO {
    /** Always positive; direction is given by {@link #type}. */
    private Double amount;
    private AdjustmentType type;
    private String description;

    public enum AdjustmentType {
        ADD, REMOVE
    }
}
