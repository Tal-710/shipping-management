package com.shippingmanagement.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class InventoryResponse {
    private String productId;
    private boolean inStock;
}
