package com.orderflow.events;

import com.orderflow.shipment.ShipmentStatus;
import java.util.UUID;

public record ShipmentEvent(UUID shipmentId, UUID orderId, ShipmentStatus status, String note) {
}
