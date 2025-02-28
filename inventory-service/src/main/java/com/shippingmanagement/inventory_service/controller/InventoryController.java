package com.shippingmanagement.inventory_service.controller;

import com.shippingmanagement.inventory_service.dto.InventoryCheckRequest;
import com.shippingmanagement.inventory_service.dto.InventoryCheckResponse;
import com.shippingmanagement.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/check")
    public ResponseEntity<InventoryCheckResponse> checkInventory(@Valid @RequestBody InventoryCheckRequest request) {
        log.info("Received inventory check request for {} items", request.getItems().size());
        InventoryCheckResponse response = inventoryService.checkAndReserveInventory(request);
        return ResponseEntity.ok(response);
    }
}