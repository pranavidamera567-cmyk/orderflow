package com.orderflow.order;

import jakarta.persistence.Embeddable;

@Embeddable
public class OrderLine {
    private String sku;
    private int quantity;

    protected OrderLine() {
    }

    public OrderLine(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
}
