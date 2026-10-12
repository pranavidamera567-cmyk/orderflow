package com.orderflow.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    public record LineRequest(@NotBlank String sku, @Min(1) int quantity) {
    }

    public record CreateOrderRequest(@NotBlank @Email String customerEmail,
                                     @NotEmpty List<@Valid LineRequest> items) {
    }

    public record OrderResponse(UUID id, String customerEmail, OrderStatus status, Instant createdAt,
                                Instant updatedAt, List<LineRequest> items) {
        static OrderResponse of(CustomerOrder o) {
            return new OrderResponse(o.getId(), o.getCustomerEmail(), o.getStatus(), o.getCreatedAt(),
                    o.getUpdatedAt(),
                    o.getLines().stream().map(l -> new LineRequest(l.getSku(), l.getQuantity())).toList());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        List<OrderLine> lines = req.items().stream().map(i -> new OrderLine(i.sku(), i.quantity())).toList();
        return OrderResponse.of(service.create(req.customerEmail(), lines));
    }

    @GetMapping
    public List<OrderResponse> search(@RequestParam(required = false) OrderStatus status,
                                      @RequestParam(required = false) String email) {
        return service.search(status, email).stream().map(OrderResponse::of).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return OrderResponse.of(service.get(id));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) {
        return OrderResponse.of(service.cancel(id));
    }
}
