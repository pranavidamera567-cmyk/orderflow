package com.orderflow.shipment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    public record TrackingResponse(ShipmentStatus status, String note, Instant at) {
    }

    public record ShipmentResponse(UUID id, UUID orderId, ShipmentStatus status, String carrier,
                                   String trackingNumber, Instant updatedAt, List<TrackingResponse> history) {
        static ShipmentResponse of(Shipment s) {
            return new ShipmentResponse(s.getId(), s.getOrderId(), s.getStatus(), s.getCarrier(),
                    s.getTrackingNumber(), s.getUpdatedAt(),
                    s.getHistory().stream().map(h -> new TrackingResponse(h.getStatus(), h.getNote(), h.getAt())).toList());
        }
    }

    public record ExceptionRequest(@NotBlank String reason) {
    }

    public record ResolveRequest(String note) {
    }

    @GetMapping
    public List<ShipmentResponse> list(@RequestParam(required = false) ShipmentStatus status) {
        return service.list(status).stream().map(ShipmentResponse::of).toList();
    }

    @GetMapping("/{id}")
    public ShipmentResponse get(@PathVariable UUID id) {
        return ShipmentResponse.of(service.get(id));
    }

    @GetMapping("/by-order/{orderId}")
    public ShipmentResponse byOrder(@PathVariable UUID orderId) {
        return ShipmentResponse.of(service.getByOrder(orderId));
    }

    /** Simulates the next carrier scan. */
    @PostMapping("/{id}/advance")
    public ShipmentResponse advance(@PathVariable UUID id) {
        return ShipmentResponse.of(service.advance(id));
    }

    @PostMapping("/{id}/exception")
    public ShipmentResponse raiseException(@PathVariable UUID id, @Valid @RequestBody ExceptionRequest req) {
        return ShipmentResponse.of(service.raiseException(id, req.reason()));
    }

    @PostMapping("/{id}/resolve")
    public ShipmentResponse resolve(@PathVariable UUID id, @RequestBody(required = false) ResolveRequest req) {
        return ShipmentResponse.of(service.resolveException(id, req == null ? null : req.note()));
    }
}
