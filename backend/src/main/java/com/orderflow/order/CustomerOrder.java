package com.orderflow.order;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class CustomerOrder {
    @Id
    private UUID id = UUID.randomUUID();
    private String customerEmail;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderLine> lines = new ArrayList<>();

    protected CustomerOrder() {
    }

    public CustomerOrder(String customerEmail, List<OrderLine> lines) {
        this.customerEmail = customerEmail;
        this.lines = new ArrayList<>(lines);
    }

    public UUID getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<OrderLine> getLines() { return lines; }

    void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
}
