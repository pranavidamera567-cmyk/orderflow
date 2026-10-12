package com.orderflow.shipment;

import com.orderflow.common.ApiException;
import com.orderflow.events.EventPublisher;
import com.orderflow.events.ShipmentEvent;
import com.orderflow.events.Topics;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentService {
    private static final List<String> CARRIERS = List.of("UPS", "FedEx", "DHL", "USPS");

    private final ShipmentRepository shipments;
    private final EventPublisher events;

    public ShipmentService(ShipmentRepository shipments, EventPublisher events) {
        this.shipments = shipments;
        this.events = events;
    }

    /** Idempotent: a redelivered ORDER_CONFIRMED event does not create a second shipment. */
    @Transactional
    public void createForOrder(UUID orderId) {
        if (shipments.findByOrderId(orderId).isPresent()) {
            return;
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Shipment s = shipments.save(new Shipment(orderId,
                CARRIERS.get(rnd.nextInt(CARRIERS.size())), "TRK" + rnd.nextLong(1_000_000_000L, 9_999_999_999L)));
        publish(s, "Shipment created");
    }

    @Transactional
    public void cancelForOrder(UUID orderId) {
        shipments.findByOrderId(orderId).ifPresent(s -> {
            if (!s.getStatus().isTerminal()) {
                s.moveTo(ShipmentStatus.CANCELLED, "Order cancelled");
                publish(s, "Order cancelled");
            }
        });
    }

    @Transactional
    public Shipment advance(UUID id) {
        Shipment s = get(id);
        ShipmentStatus next = s.getStatus().next();
        if (next == null) {
            throw ApiException.conflict("Shipment is " + s.getStatus() + " and cannot be advanced");
        }
        s.moveTo(next, "Status updated to " + next);
        publish(s, "Status updated to " + next);
        return s;
    }

    @Transactional
    public Shipment raiseException(UUID id, String reason) {
        Shipment s = get(id);
        if (s.getStatus().isTerminal() || s.getStatus() == ShipmentStatus.EXCEPTION) {
            throw ApiException.conflict("Shipment is " + s.getStatus() + " and cannot raise an exception");
        }
        s.moveTo(ShipmentStatus.EXCEPTION, reason);
        publish(s, reason);
        return s;
    }

    /** Resumes the shipment from the status it had before the exception. */
    @Transactional
    public Shipment resolveException(UUID id, String note) {
        Shipment s = get(id);
        if (s.getStatus() != ShipmentStatus.EXCEPTION) {
            throw ApiException.conflict("Shipment has no open exception");
        }
        String message = (note == null || note.isBlank()) ? "Exception resolved" : note;
        s.moveTo(s.getStatusBeforeException(), message);
        publish(s, message);
        return s;
    }

    @Transactional(readOnly = true)
    public Shipment get(UUID id) {
        return shipments.findById(id).orElseThrow(() -> ApiException.notFound("Shipment " + id));
    }

    @Transactional(readOnly = true)
    public Shipment getByOrder(UUID orderId) {
        return shipments.findByOrderId(orderId).orElseThrow(() -> ApiException.notFound("Shipment for order " + orderId));
    }

    @Transactional(readOnly = true)
    public List<Shipment> list(ShipmentStatus status) {
        return status == null ? shipments.findAllByOrderByUpdatedAtDesc()
                : shipments.findByStatusInOrderByUpdatedAtDesc(List.of(status));
    }

    private void publish(Shipment s, String note) {
        events.publish(Topics.SHIPMENT_EVENTS, s.getOrderId(),
                new ShipmentEvent(s.getId(), s.getOrderId(), s.getStatus(), note));
    }
}
