package com.orderflow.order;

public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, EXCEPTION, CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == SHIPPED || next == EXCEPTION || next == CANCELLED;
            case SHIPPED -> next == DELIVERED || next == EXCEPTION;
            case EXCEPTION -> next == CONFIRMED || next == SHIPPED || next == CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
