package com.shippingmanagement.inventory_service.controller;

import com.shippingmanagement.inventory_service.dto.InventoryResponse;
import com.shippingmanagement.inventory_service.dto.OrderItemRequestDto;
import com.shippingmanagement.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/check")
    public ResponseEntity<List<InventoryResponse>> checkInventory(@RequestBody List<OrderItemRequestDto> orderItems) {
        log.info("Received request to check inventory");
        List<InventoryResponse> inventoryResponses = inventoryService.checkAndReduceInventory(orderItems);
        if (inventoryResponses.stream().anyMatch(response -> !response.isInStock())) {
            log.info("Product not enough in inventory");
            return ResponseEntity.badRequest().body(inventoryResponses);
        }

        return ResponseEntity.ok(inventoryResponses);
    }
}