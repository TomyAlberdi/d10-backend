package d10.backend.Mapper;

import java.time.LocalDate;

import d10.backend.DTO.Order.OrderDTO;
import d10.backend.Model.Order;
import d10.backend.Model.Product;

public class OrderMapper {

    private OrderMapper() {
        // Utility class
    }

    public static Order toEntity(Product product, Integer saleUnitQuantity, LocalDate orderDate, String detail) {
        Order order = new Order();
        order.setReceived(false);
        updateFromProduct(order, product, saleUnitQuantity, orderDate, detail);
        return order;
    }

    /**
     * Refreshes the editable fields of an order. The product snapshot is rewritten
     * too, so editing an order picks up any rename done since it was created.
     */
    public static void updateFromProduct(Order order, Product product, Integer saleUnitQuantity, LocalDate orderDate,
            String detail) {
        order.setProductId(product.getId());
        order.setProductCode(product.getCode());
        order.setProductName(product.getName());
        order.setProviderName(product.getProviderName());
        order.setSaleUnitType(product.getSaleUnitType());
        order.setSaleUnitQuantity(saleUnitQuantity);
        order.setOrderDate(orderDate != null ? orderDate : LocalDate.now());
        order.setDetail(detail != null && !detail.trim().isEmpty() ? detail.trim() : null);
    }

    public static OrderDTO toDTO(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderDTO(
                order.getId(),
                order.getProductId(),
                order.getProductCode(),
                order.getProductName(),
                order.getProviderName(),
                order.getSaleUnitQuantity(),
                order.getSaleUnitType(),
                order.getOrderDate(),
                order.getReceived() != null && order.getReceived(),
                order.getReceivedDatetime(),
                order.getDetail());
    }

}
