package com.orderflow.order;

import com.orderflow.common.ApiException;
import com.orderflow.events.EventPublisher;
import com.orderflow.events.OrderEvent;
import com.orderflow.events.Topics;
import com.orderflow.inventory.InventoryService;
import com.orderflow.shipment.ShipmentStatus;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final InventoryService inventory;
    private final EventPublisher events;

    public OrderService(OrderRepository orders, InventoryService inventory, EventPublisher events) {
        this.orders = orders;
        this.inventory = inventory;
        this.events = events;
    }

    /** Validates, reserves stock for every line (all-or-nothing) and confirms the order. */
    @Transactional
    public CustomerOrder create(String email, List<OrderLine> lines) {
        for (OrderLine line : lines) {
            inventory.reserve(line.getSku(), line.getQuantity());
        }
        CustomerOrder order = orders.save(new CustomerOrder(email, lines));
        transition(order, OrderStatus.CONFIRMED);
        events.publish(Topics.ORDER_EVENTS, order.getId(),
                new OrderEvent(OrderEvent.Type.ORDER_CONFIRMED, order.getId()));
        return order;
    }

    @Transactional
    public CustomerOrder cancel(UUID id) {
        CustomerOrder order = get(id);
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw ApiException.conflict("Cannot cancel an order that is " + order.getStatus());
        }
        order.getLines().forEach(l -> inventory.release(l.getSku(), l.getQuantity()));
        transition(order, OrderStatus.CANCELLED);
        events.publish(Topics.ORDER_EVENTS, id, new OrderEvent(OrderEvent.Type.ORDER_CANCELLED, id));
        return order;
    }

    /** Maps a shipment status update onto the order lifecycle; stale or irrelevant updates are ignored. */
    @Transactional
    public void applyShipmentUpdate(UUID orderId, ShipmentStatus shipmentStatus) {
        CustomerOrder order = orders.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Shipment update for unknown order {}", orderId);
            return;
        }
        OrderStatus target = switch (shipmentStatus) {
            case CREATED -> OrderStatus.CONFIRMED;
            case PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY -> OrderStatus.SHIPPED;
            case DELIVERED -> OrderStatus.DELIVERED;
            case EXCEPTION -> OrderStatus.EXCEPTION;
            case CANCELLED -> null;
        };
        if (target == null || target == order.getStatus()) {
            return;
        }
        if (!order.getStatus().canTransitionTo(target)) {
            log.warn("Ignoring shipment status {} for order {} in state {}", shipmentStatus, orderId, order.getStatus());
            return;
        }
        transition(order, target);
        if (target == OrderStatus.DELIVERED) {
            order.getLines().forEach(l -> inventory.fulfil(l.getSku(), l.getQuantity()));
        }
    }

    @Transactional(readOnly = true)
    public CustomerOrder get(UUID id) {
        return orders.findById(id).orElseThrow(() -> ApiException.notFound("Order " + id));
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> search(OrderStatus status, String email) {
        if (status != null) {
            return orders.findByStatusOrderByCreatedAtDesc(status);
        }
        if (email != null && !email.isBlank()) {
            return orders.findByCustomerEmailContainingIgnoreCaseOrderByCreatedAtDesc(email);
        }
        return orders.findAllByOrderByCreatedAtDesc();
    }

    private void transition(CustomerOrder order, OrderStatus next) {
        if (!order.getStatus().canTransitionTo(next)) {
            throw ApiException.conflict("Illegal order transition " + order.getStatus() + " -> " + next);
        }
        order.setStatus(next);
    }
}
