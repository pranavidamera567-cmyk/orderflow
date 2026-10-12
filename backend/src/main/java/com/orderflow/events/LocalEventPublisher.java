package com.orderflow.events;

import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Dispatches events in-process (no broker); used by the test profile. */
@Component
@Profile("test")
class LocalEventPublisher implements EventPublisher {
    private final EventHandlers handlers;

    LocalEventPublisher(@Lazy EventHandlers handlers) {
        this.handlers = handlers;
    }

    @Override
    public void publish(String topic, UUID key, Object event) {
        AfterCommit.run(() -> {
            if (event instanceof OrderEvent e) {
                handlers.onOrderEvent(e);
            } else if (event instanceof ShipmentEvent e) {
                handlers.onShipmentEvent(e);
            }
        });
    }
}
