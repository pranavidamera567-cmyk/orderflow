package com.orderflow.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
class KafkaListeners {
    private final EventHandlers handlers;
    private final ObjectMapper mapper;

    KafkaListeners(EventHandlers handlers, ObjectMapper mapper) {
        this.handlers = handlers;
        this.mapper = mapper;
    }

    @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "orderflow-shipments")
    void onOrderEvent(String payload) throws JsonProcessingException {
        handlers.onOrderEvent(mapper.readValue(payload, OrderEvent.class));
    }

    @KafkaListener(topics = Topics.SHIPMENT_EVENTS, groupId = "orderflow-orders")
    void onShipmentEvent(String payload) throws JsonProcessingException {
        handlers.onShipmentEvent(mapper.readValue(payload, ShipmentEvent.class));
    }
}
