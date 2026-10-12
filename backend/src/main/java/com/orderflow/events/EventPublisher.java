package com.orderflow.events;

import java.util.UUID;

public interface EventPublisher {
    /** Publishes after the surrounding transaction commits, so consumers always see persisted state. */
    void publish(String topic, UUID key, Object event);
}
