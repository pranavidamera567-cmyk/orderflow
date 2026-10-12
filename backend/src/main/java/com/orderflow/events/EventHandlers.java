package com.orderflow.events;

import com.orderflow.order.OrderService;
import com.orderflow.shipment.ShipmentService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Transport-independent event handling, shared by the Kafka listeners and the in-process test publisher. */
@Component
public class EventHandlers {
    private final ShipmentService shipments;
    private final OrderService orders;

    public EventHandlers(ShipmentService shipments, OrderService orders) {
        this.shipments = shipments;
        this.orders = orders;
    }

    // REQUIRES_NEW: in-process dispatch runs in afterCommit, where the publisher's transaction is already finished
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderEvent(OrderEvent event) {
        switch (event.type()) {
            case ORDER_CONFIRMED -> shipments.createForOrder(event.orderId());
            case ORDER_CANCELLED -> shipments.cancelForOrder(event.orderId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onShipmentEvent(ShipmentEvent event) {
        orders.applyShipmentUpdate(event.orderId(), event.status());
    }
}
