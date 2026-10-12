package com.orderflow.shipment;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;

@Embeddable
public class TrackingEntry {
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;
    private String note;
    private Instant at = Instant.now();

    protected TrackingEntry() {
    }

    public TrackingEntry(ShipmentStatus status, String note) {
        this.status = status;
        this.note = note;
    }

    public ShipmentStatus getStatus() { return status; }
    public String getNote() { return note; }
    public Instant getAt() { return at; }
}
