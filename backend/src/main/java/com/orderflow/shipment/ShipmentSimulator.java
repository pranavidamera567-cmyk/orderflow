package com.orderflow.shipment;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Emits a carrier scan for every in-flight shipment on a timer, simulating asynchronous notifications. */
@Component
@ConditionalOnProperty(name = "shipments.simulator.enabled", havingValue = "true")
class ShipmentSimulator {
    private static final List<ShipmentStatus> IN_FLIGHT = List.of(
            ShipmentStatus.CREATED, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY);

    private final ShipmentRepository repo;
    private final ShipmentService service;

    ShipmentSimulator(ShipmentRepository repo, ShipmentService service) {
        this.repo = repo;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${shipments.simulator.interval-ms}")
    void tick() {
        repo.findByStatusInOrderByUpdatedAtDesc(IN_FLIGHT).forEach(s -> service.advance(s.getId()));
    }
}
