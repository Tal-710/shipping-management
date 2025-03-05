package com.shippingmanagement.inventory_service.service;

import com.shippingmanagement.inventory_service.dto.InventoryResponse;
import com.shippingmanagement.inventory_service.dto.OrderItemRequestDto;
import com.shippingmanagement.inventory_service.model.Inventory;
import com.shippingmanagement.inventory_service.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public List<InventoryResponse> checkAndReduceInventory(List<OrderItemRequestDto> orderItems) {
        log.info("Checking and reducing inventory");
        List<InventoryResponse> inventoryResponses = orderItems.stream()
                .map(this::checkSingleProductAvailability)
                .collect(Collectors.toList());


        if (inventoryResponses.stream().allMatch(InventoryResponse::isInStock)) {
            log.info("All inventory items in stock");
            orderItems.forEach(this::reduceProductQuantity);
        }

        return inventoryResponses;
    }

    private InventoryResponse checkSingleProductAvailability(OrderItemRequestDto orderItem) {
        Inventory inventory = inventoryRepository.findByProductId(orderItem.getProductId());
        log.info("Product: {} Quantity: {}", orderItem.getProductId(), inventory.getQuantity());
        if (inventory == null || inventory.getQuantity() < orderItem.getQuantity()) {
            log.info("Product not in stock");
            return new InventoryResponse(
                    String.valueOf(orderItem.getProductId()),
                    false
            );
        }

        log.info("Product availability ok");
        return new InventoryResponse(String.valueOf(orderItem.getProductId()), true);
    }

    private void reduceProductQuantity(OrderItemRequestDto orderItem) {
        Inventory inventory = inventoryRepository.findByProductId(orderItem.getProductId());
        if (inventory != null && inventory.getQuantity() >= orderItem.getQuantity()) {
            log.info("Reduce product quantity");
            inventory.setQuantity(inventory.getQuantity() - orderItem.getQuantity());
            inventoryRepository.save(inventory);
        }
    }
}
