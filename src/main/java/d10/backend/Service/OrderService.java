package d10.backend.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import d10.backend.DTO.Order.CreateOrderDTO;
import d10.backend.DTO.Order.CreateOrderProductDTO;
import d10.backend.DTO.Order.OrderDTO;
import d10.backend.Exception.ResourceNotFoundException;
import d10.backend.Mapper.OrderMapper;
import d10.backend.Model.Order;
import d10.backend.Model.OrderProduct;
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

    // Newest order first; two orders of the same day fall back to the insertion
    // order, which is how the printed list is read.
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "date", "id");

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public List<OrderDTO> findAll() {
        return orderRepository.findAll(NEWEST_FIRST).stream()
                .map(OrderMapper::toDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO findById(String id) {
        return OrderMapper.toDTO(findEntityById(id));
    }

    public OrderDTO createOrder(CreateOrderDTO createOrderDTO) {
        Order order = new Order();
        order.setReceived(false);
        order.setDate(createOrderDTO.getDate() != null ? createOrderDTO.getDate() : LocalDate.now());
        order.setProducts(resolveProducts(createOrderDTO));
        return OrderMapper.toDTO(orderRepository.save(order));
    }

    /**
     * Replaces the date and the whole product list of a pending order. The
     * product snapshots are rewritten, so editing picks up any rename done since
     * the order was created.
     */
    public OrderDTO updateOrder(String id, CreateOrderDTO createOrderDTO) {
        Order order = findEntityById(id);
        if (isReceived(order)) {
            throw new IllegalStateException(
                    "El pedido ya fue recibido. Marcalo como pendiente antes de editarlo.");
        }
        order.setDate(createOrderDTO.getDate() != null ? createOrderDTO.getDate() : order.getDate());
        order.setProducts(resolveProducts(createOrderDTO));
        return OrderMapper.toDTO(orderRepository.save(order));
    }

    /**
     * Receives the whole order, adding the sale units of every line to the stock
     * of its product, or sets it back to pending taking them out again.
     */
    public OrderDTO updateReceived(String id, boolean received) {
        Order order = findEntityById(id);
        if (isReceived(order) == received) {
            return OrderMapper.toDTO(order);
        }
        List<OrderProduct> lines = linesWithQuantity(order);
        // There are no multi document transactions here, so everything that can
        // fail is checked up front: a deleted product, one whose stock data is
        // incomplete, or not enough stock left to give back, aborts the order
        // before any movement is written. Otherwise a failure halfway through
        // would leave the order pending with part of its stock already moved.
        for (OrderProduct line : lines) {
            productService.checkStockUpdatable(line.getProductId());
            if (!received) {
                productService.checkStockSufficient(line.getProductId(), line.getSaleUnitQuantity());
            }
        }
        for (OrderProduct line : lines) {
            if (received) {
                productService.updateStockIncrease(
                        line.getProductId(), line.getSaleUnitQuantity(), LocalDate.now(), RECEIVED_DETAIL);
            } else {
                productService.updateStockDecrease(
                        line.getProductId(), line.getSaleUnitQuantity(), LocalDate.now(), REVERTED_DETAIL);
            }
        }
        order.setReceived(received);
        return OrderMapper.toDTO(orderRepository.save(order));
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

    /**
     * Validates the incoming lines and turns them into product snapshots. A
     * product can only appear once, so receiving and reverting can check every
     * line against the stock on its own.
     */
    private List<OrderProduct> resolveProducts(CreateOrderDTO createOrderDTO) {
        List<CreateOrderProductDTO> items = createOrderDTO.getProducts();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un producto.");
        }
        Set<String> seen = new HashSet<>();
        List<OrderProduct> products = new ArrayList<>();
        for (CreateOrderProductDTO item : items) {
            if (item.getProductId() == null || item.getProductId().trim().isEmpty()) {
                throw new IllegalArgumentException("El producto del pedido es obligatorio.");
            }
            if (item.getSaleUnitQuantity() == null || item.getSaleUnitQuantity() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada producto debe ser mayor a 0.");
            }
            if (!seen.add(item.getProductId())) {
                throw new IllegalArgumentException("El pedido no puede repetir el mismo producto.");
            }
            Product product = productService.findById(item.getProductId());
            products.add(OrderMapper.toProduct(product, item.getSaleUnitQuantity(), item.getDetail()));
        }
        return products;
    }

    /** Lines that actually move stock: a missing or non positive amount is skipped. */
    private List<OrderProduct> linesWithQuantity(Order order) {
        List<OrderProduct> lines = new ArrayList<>();
        if (order.getProducts() == null) {
            return lines;
        }
        for (OrderProduct line : order.getProducts()) {
            Integer quantity = line.getSaleUnitQuantity();
            if (quantity != null && quantity > 0) {
                lines.add(line);
            }
        }
        return lines;
    }

    private boolean isReceived(Order order) {
        return Boolean.TRUE.equals(order.getReceived());
    }

}
