package com.orderflow.events;

import java.util.UUID;

public record OrderEvent(Type type, UUID orderId) {
    public enum Type { ORDER_CONFIRMED, ORDER_CANCELLED }
}
