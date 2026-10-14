package d10.backend.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import d10.backend.DTO.Order.CreateOrderDTO;
import d10.backend.DTO.Order.CreateOrderProductDTO;
import d10.backend.DTO.Order.OrderDTO;
import d10.backend.DTO.Order.RestockProductDTO;
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

    /**
     * Every active product that ran out of stock, along with how much of it is
     * already waiting in pending orders.
     */
    public List<RestockProductDTO> findRestockProducts() {
        Map<String, Integer> pendingByProduct = new LinkedHashMap<>();
        for (Order order : orderRepository.findAll()) {
            if (isReceived(order)) {
                continue;
            }
            for (OrderProduct line : linesWithQuantity(order)) {
                ProductService.addRequired(pendingByProduct, line.getProductId(), line.getSaleUnitQuantity());
            }
        }
        List<RestockProductDTO> products = new ArrayList<>();
        for (Product product : productService.getOutOfStockProducts()) {
            Integer stock = product.getStock() != null ? product.getStock().getQuantity() : null;
            products.add(new RestockProductDTO(
                    product.getId(),
                    product.getCode(),
                    product.getName(),
                    product.getProviderName(),
                    product.getSaleUnitType(),
                    stock != null ? stock : 0,
                    pendingByProduct.getOrDefault(product.getId(), 0)));
        }
        return products;
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
    public OrderDTO updateReceived(String id, boolean received, boolean allowNegativeStock) {
        Order order = findEntityById(id);
        if (isReceived(order) == received) {
            return OrderMapper.toDTO(order);
        }
        List<OrderProduct> lines = linesWithQuantity(order);
        // There are no multi document transactions here, so everything that can
        // fail is checked up front: a deleted product, one whose stock data is
        // incomplete, or not enough stock left to give back (unless the user
        // agreed to go negative), aborts the order before any movement is
        // written. Otherwise a failure halfway through would leave the order
        // pending with part of its stock already moved.
        Map<String, Integer> required = new LinkedHashMap<>();
        for (OrderProduct line : lines) {
            productService.checkStockUpdatable(line.getProductId());
            ProductService.addRequired(required, line.getProductId(), line.getSaleUnitQuantity());
        }
        if (!received) {
            productService.checkNegativeStock(required, allowNegativeStock);
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

    /**
     * Appends lines to a pending order, leaving the ones it already has (and
     * their snapshots) untouched. A product already in the order is rejected
     * rather than merged, so no amount changes behind the user's back.
     */
    public OrderDTO addProducts(String id, List<CreateOrderProductDTO> items) {
        Order order = findEntityById(id);
        if (isReceived(order)) {
            throw new IllegalStateException(
                    "El pedido ya fue recibido. Marcalo como pendiente antes de agregarle productos.");
        }
        List<OrderProduct> products = order.getProducts() != null
                ? new ArrayList<>(order.getProducts())
                : new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (OrderProduct line : products) {
            seen.add(line.getProductId());
        }
        products.addAll(resolveProducts(items, seen));
        order.setProducts(products);
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
        return resolveProducts(createOrderDTO.getProducts(), new HashSet<>());
    }

    /** Same as above, also rejecting the products in {@code seen} (the lines an order already has). */
    private List<OrderProduct> resolveProducts(List<CreateOrderProductDTO> items, Set<String> seen) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un producto.");
        }
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
