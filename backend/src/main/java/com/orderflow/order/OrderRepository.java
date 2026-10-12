package com.orderflow.order;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {
    List<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<CustomerOrder> findByCustomerEmailContainingIgnoreCaseOrderByCreatedAtDesc(String email);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}
