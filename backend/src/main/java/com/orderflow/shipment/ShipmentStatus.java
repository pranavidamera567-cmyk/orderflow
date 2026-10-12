package com.orderflow.shipment;

public enum ShipmentStatus {
    CREATED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, EXCEPTION, CANCELLED;

    /** Next step of the happy path, or null when there is none. */
    public ShipmentStatus next() {
        return switch (this) {
            case CREATED -> PICKED_UP;
            case PICKED_UP -> IN_TRANSIT;
            case IN_TRANSIT -> OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> DELIVERED;
            case DELIVERED, EXCEPTION, CANCELLED -> null;
        };
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
