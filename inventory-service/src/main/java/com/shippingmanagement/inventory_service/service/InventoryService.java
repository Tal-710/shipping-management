package com.shippingmanagement.inventory_service.service;

import com.shippingmanagement.inventory_service.dto.InventoryCheckRequest;
import com.shippingmanagement.inventory_service.dto.InventoryCheckResponse;
import com.shippingmanagement.inventory_service.dto.InventoryItemRequest;
import com.shippingmanagement.inventory_service.model.Inventory;
import com.shippingmanagement.inventory_service.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public InventoryCheckResponse checkAndReserveInventory(InventoryCheckRequest request) {
        log.info("Checking inventory for {} items", request.getItems().size());

        List<String> unavailableItems = new ArrayList<>();
        boolean allAvailable = true;

        // First, check if all items are available
        for (InventoryItemRequest item : request.getItems()) {
            Optional<Inventory> inventoryOptional = inventoryRepository.findByProductId(item.getProductId());

            if (inventoryOptional.isEmpty()) {
                log.warn("Product {} not found in inventory", item.getProductId());
                unavailableItems.add("Product ID " + item.getProductId() + " not found");
                allAvailable = false;
                continue;
            }

            Inventory inventory = inventoryOptional.get();
            if (inventory.getQuantityAvailable() < item.getQuantity()) {
                log.warn("Insufficient inventory for product {}. Requested: {}, Available: {}",
                        item.getProductId(), item.getQuantity(), inventory.getQuantityAvailable());

                unavailableItems.add("Product ID " + item.getProductId() +
                        " has insufficient quantity (available: " + inventory.getQuantityAvailable() +
                        ", requested: " + item.getQuantity() + ")");

                allAvailable = false;
            }
        }

        // If all available and reservation requested, reserve the inventory
        if (allAvailable && request.isReserve()) {
            for (InventoryItemRequest item : request.getItems()) {
                Inventory inventory = inventoryRepository.findByProductId(item.getProductId()).get();
                inventory.setQuantityAvailable(inventory.getQuantityAvailable() - item.getQuantity());
                inventoryRepository.save(inventory);
                log.info("Reserved {} units of product {}", item.getQuantity(), item.getProductId());
            }
        }

        return InventoryCheckResponse.builder()
                .available(allAvailable)
                .unavailableItems(unavailableItems)
                .build();
    }
}
