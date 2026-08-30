package d10.backend.Mapper;

import java.util.ArrayList;

import d10.backend.DTO.Order.OrderDTO;
import d10.backend.Model.Order;
import d10.backend.Model.OrderProduct;
import d10.backend.Model.Product;

public class OrderMapper {

    private OrderMapper() {
        // Utility class
    }

    /** Snapshots the product data that the order line has to keep on its own. */
    public static OrderProduct toProduct(Product product, Integer saleUnitQuantity, String detail) {
        return new OrderProduct(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getProviderName(),
                product.getSaleUnitType(),
                saleUnitQuantity,
                detail != null && !detail.trim().isEmpty() ? detail.trim() : null);
    }

    public static OrderDTO toDTO(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderDTO(
                order.getId(),
                order.getDate(),
                Boolean.TRUE.equals(order.getReceived()),
                order.getProducts() != null ? order.getProducts() : new ArrayList<>());
    }

}
