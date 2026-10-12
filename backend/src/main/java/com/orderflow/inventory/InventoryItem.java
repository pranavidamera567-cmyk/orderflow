package com.orderflow.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class InventoryItem {
    @Id
    private String sku;
    private String name;
    private int onHand;
    private int reserved;

    protected InventoryItem() {
    }

    public InventoryItem(String sku, String name, int onHand) {
        this.sku = sku;
        this.name = name;
        this.onHand = onHand;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public int getOnHand() { return onHand; }
    public int getReserved() { return reserved; }
    public int available() { return onHand - reserved; }

    void reserve(int qty) { reserved += qty; }
    void release(int qty) { reserved = Math.max(0, reserved - qty); }
    void fulfil(int qty) { reserved = Math.max(0, reserved - qty); onHand = Math.max(0, onHand - qty); }
    void restock(int qty) { onHand += qty; }
}
