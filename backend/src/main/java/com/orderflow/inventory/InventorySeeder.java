package com.orderflow.inventory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class InventorySeeder implements CommandLineRunner {
    private final InventoryRepository repo;

    InventorySeeder(InventoryRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) {
            return;
        }
        repo.save(new InventoryItem("SKU-1001", "Wireless Mouse", 100));
        repo.save(new InventoryItem("SKU-1002", "Mechanical Keyboard", 50));
        repo.save(new InventoryItem("SKU-1003", "27\" Monitor", 20));
        repo.save(new InventoryItem("SKU-1004", "USB-C Hub", 5));
    }
}
