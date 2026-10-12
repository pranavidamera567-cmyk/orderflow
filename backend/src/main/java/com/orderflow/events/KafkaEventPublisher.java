package com.orderflow.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    KafkaEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.kafka = kafka;
        this.mapper = mapper;
    }

    @Override
    public void publish(String topic, UUID key, Object event) {
        String json;
        try {
            json = mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise " + event, e);
        }
        // keyed by order id so all events of one order stay ordered within a partition
        AfterCommit.run(() -> kafka.send(topic, key.toString(), json));
    }
}
