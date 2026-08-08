package d10.backend.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import d10.backend.DTO.Order.CreateOrderDTO;
import d10.backend.DTO.Order.OrderDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.OrderMapper;
import d10.backend.Model.Order;
import d10.backend.Model.Product;
import d10.backend.Repository.OrderRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class OrderService {

    /** Detail written to the stock log when an order is received. */
    private static final String RECEIVED_DETAIL = "Pedido recibido";
    /** Detail written to the stock log when a received order is set back to pending. */
    private static final String REVERTED_DETAIL = "Pedido recibido (revertido)";

    // Most recent batch first; inside a batch the orders keep the order in which
    // they were written down, which is how the printed list is read.
    private static final Sort NEWEST_BATCH_FIRST = Sort.by(Sort.Direction.DESC, "orderDate")
            .and(Sort.by(Sort.Direction.ASC, "id"));
    private static final Sort AS_ENTERED = Sort.by(Sort.Direction.ASC, "id");

    private final OrderRepository orderRepository;
    private final ProductService productService;

    /**
     * Orders filtered by batch date and/or received flag. Both filters are optional.
     */
    public List<OrderDTO> findAll(LocalDate orderDate, Boolean received) {
        List<Order> orders;
        if (orderDate != null && received != null) {
            orders = orderRepository.findByOrderDateAndReceived(orderDate, received, AS_ENTERED);
        } else if (orderDate != null) {
            orders = orderRepository.findByOrderDate(orderDate, AS_ENTERED);
        } else if (received != null) {
            orders = orderRepository.findByReceived(received, NEWEST_BATCH_FIRST);
        } else {
            orders = orderRepository.findAll(NEWEST_BATCH_FIRST);
        }
        return orders.stream().map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    /**
     * Dates that have at least one order, most recent first. Feeds the batch
     * selector of the orders page.
     */
    public List<LocalDate> getOrderDates() {
        List<LocalDate> dates = new ArrayList<>();
        for (Order order : orderRepository.findAll()) {
            LocalDate date = order.getOrderDate();
            if (date != null && !dates.contains(date)) {
                dates.add(date);
            }
        }
        dates.sort(Comparator.reverseOrder());
        return dates;
    }

    public OrderDTO findById(String id) {
        return OrderMapper.toDTO(findEntityById(id));
    }

    public List<OrderDTO> findByProductId(String productId) {
        return orderRepository.findByProductId(productId, NEWEST_BATCH_FIRST).stream()
                .map(OrderMapper::toDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO createOrder(CreateOrderDTO createOrderDTO) {
        Product product = resolveProduct(createOrderDTO);
        Order order = OrderMapper.toEntity(
                product,
                createOrderDTO.getSaleUnitQuantity(),
                createOrderDTO.getOrderDate(),
                createOrderDTO.getDetail());
        return OrderMapper.toDTO(orderRepository.save(order));
    }

    public OrderDTO updateOrder(String id, CreateOrderDTO createOrderDTO) {
        Order order = findEntityById(id);
        if (Boolean.TRUE.equals(order.getReceived())) {
            throw new IllegalStateException(
                    "El pedido ya fue recibido. Marcalo como pendiente antes de editarlo.");
        }
        Product product = resolveProduct(createOrderDTO);
        OrderMapper.updateFromProduct(
                order,
                product,
                createOrderDTO.getSaleUnitQuantity(),
                createOrderDTO.getOrderDate(),
                createOrderDTO.getDetail());
        return OrderMapper.toDTO(orderRepository.save(order));
    }

    /**
     * Marks a single order as received (adding its sale units to the stock of the
     * product) or back as pending (taking them out again).
     */
    public OrderDTO updateReceived(String id, boolean received) {
        Order order = findEntityById(id);
        if (isReceived(order) == received) {
            return OrderMapper.toDTO(order);
        }
        applyStockMovement(order, received);
        return OrderMapper.toDTO(orderRepository.save(markReceived(order, received)));
    }

    /**
     * Receives every pending order of a batch, adding the sale units of each one
     * to the stock of its product.
     */
    public List<OrderDTO> receiveByDate(LocalDate orderDate) {
        List<Order> pending = orderRepository.findByOrderDateAndReceived(orderDate, false, AS_ENTERED);
        if (pending.isEmpty()) {
            throw new ResourceNotFoundException("No hay pedidos pendientes para la fecha " + orderDate + ".");
        }
        // There are no multi document transactions here, so every product is
        // resolved up front: a missing one aborts the batch before any stock moves.
        for (Order order : pending) {
            productService.findById(order.getProductId());
        }
        List<OrderDTO> receivedOrders = new ArrayList<>();
        for (Order order : pending) {
            applyStockMovement(order, true);
            receivedOrders.add(OrderMapper.toDTO(orderRepository.save(markReceived(order, true))));
        }
        return receivedOrders;
    }

    public void deleteOrder(String id) {
        Order order = findEntityById(id);
        if (isReceived(order)) {
            throw new IllegalStateException(
                    "El pedido ya fue recibido y su stock fue cargado. Marcalo como pendiente antes de eliminarlo.");
        }
        orderRepository.deleteById(id);
    }

    private Order findEntityById(String id) {
        Optional<Order> orderSearch = orderRepository.findById(id);
        if (orderSearch.isEmpty()) {
            throw new ResourceNotFoundException("Pedido con ID " + id + " no encontrado.");
        }
        return orderSearch.get();
    }

    private Product resolveProduct(CreateOrderDTO createOrderDTO) {
        if (createOrderDTO.getProductId() == null || createOrderDTO.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("El producto del pedido es obligatorio.");
        }
        if (createOrderDTO.getSaleUnitQuantity() == null || createOrderDTO.getSaleUnitQuantity() <= 0) {
            throw new IllegalArgumentException("La cantidad del pedido debe ser mayor a 0.");
        }
        return productService.findById(createOrderDTO.getProductId());
    }

    /** Moves the ordered sale units in or out of the stock of the product. */
    private void applyStockMovement(Order order, boolean received) {
        Integer quantity = order.getSaleUnitQuantity();
        if (quantity == null || quantity <= 0) {
            return;
        }
        if (received) {
            productService.updateStockIncrease(order.getProductId(), quantity, LocalDate.now(), RECEIVED_DETAIL);
        } else {
            productService.updateStockDecrease(order.getProductId(), quantity, LocalDate.now(), REVERTED_DETAIL);
        }
    }

    private Order markReceived(Order order, boolean received) {
        order.setReceived(received);
        order.setReceivedDatetime(received ? LocalDateTime.now() : null);
        return order;
    }

    private boolean isReceived(Order order) {
        return Boolean.TRUE.equals(order.getReceived());
    }

}
