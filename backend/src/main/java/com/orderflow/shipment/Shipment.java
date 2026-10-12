package com.orderflow.shipment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Shipment {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(unique = true)
    private UUID orderId;
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status = ShipmentStatus.CREATED;
    @Enumerated(EnumType.STRING)
    private ShipmentStatus statusBeforeException;
    private String carrier;
    private String trackingNumber;
    private Instant updatedAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shipment_tracking", joinColumns = @JoinColumn(name = "shipment_id"))
    @OrderBy("at ASC")
    private List<TrackingEntry> history = new ArrayList<>();

    protected Shipment() {
    }

    public Shipment(UUID orderId, String carrier, String trackingNumber) {
        this.orderId = orderId;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.history.add(new TrackingEntry(ShipmentStatus.CREATED, "Shipment created"));
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public ShipmentStatus getStatus() { return status; }
    public ShipmentStatus getStatusBeforeException() { return statusBeforeException; }
    public String getCarrier() { return carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<TrackingEntry> getHistory() { return history; }

    void moveTo(ShipmentStatus next, String note) {
        if (next == ShipmentStatus.EXCEPTION) {
            statusBeforeException = status;
        }
        status = next;
        updatedAt = Instant.now();
        history.add(new TrackingEntry(next, note));
    }
}
