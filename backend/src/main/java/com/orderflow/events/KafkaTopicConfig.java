package com.orderflow.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("!test")
class KafkaTopicConfig {
    @Bean
    NewTopic orderEvents() {
        return TopicBuilder.name(Topics.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic shipmentEvents() {
        return TopicBuilder.name(Topics.SHIPMENT_EVENTS).partitions(3).replicas(1).build();
    }
}
