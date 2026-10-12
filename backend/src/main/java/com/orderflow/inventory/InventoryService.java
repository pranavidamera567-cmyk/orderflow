package com.orderflow.inventory;

import com.orderflow.common.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryRepository repo;

    public InventoryService(InventoryRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> list() {
        return repo.findAll();
    }

    /** Reserves stock; throws 422 for unknown SKU and 409 when stock is insufficient. */
    @Transactional
    public void reserve(String sku, int qty) {
        InventoryItem item = repo.findForUpdate(sku)
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown SKU " + sku));
        if (item.available() < qty) {
            throw ApiException.conflict("Insufficient stock for " + sku + ": requested " + qty
                    + ", available " + item.available());
        }
        item.reserve(qty);
    }

    @Transactional
    public void release(String sku, int qty) {
        repo.findForUpdate(sku).ifPresent(i -> i.release(qty));
    }

    /** Called on delivery: the reserved units leave the warehouse. */
    @Transactional
    public void fulfil(String sku, int qty) {
        repo.findForUpdate(sku).ifPresent(i -> i.fulfil(qty));
    }

    @Transactional
    public InventoryItem restock(String sku, int qty) {
        InventoryItem item = repo.findForUpdate(sku).orElseThrow(() -> ApiException.notFound("SKU " + sku));
        item.restock(qty);
        return item;
    }
}
