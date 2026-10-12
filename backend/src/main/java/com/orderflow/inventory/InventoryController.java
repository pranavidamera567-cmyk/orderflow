package com.orderflow.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    public record ItemResponse(String sku, String name, int onHand, int reserved, int available) {
        static ItemResponse of(InventoryItem i) {
            return new ItemResponse(i.getSku(), i.getName(), i.getOnHand(), i.getReserved(), i.available());
        }
    }

    public record RestockRequest(@Min(1) int quantity) {
    }

    @GetMapping
    public List<ItemResponse> list() {
        return service.list().stream().map(ItemResponse::of).toList();
    }

    @PostMapping("/{sku}/restock")
    public ItemResponse restock(@PathVariable String sku, @Valid @RequestBody RestockRequest req) {
        return ItemResponse.of(service.restock(sku, req.quantity()));
    }
}
