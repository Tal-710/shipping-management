package com.shippingmanagement.inventory_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheckRequest {

    @NotEmpty(message = "Items must not be empty")
    @Valid
    private List<InventoryItemRequest> items;

    private boolean reserve = false;
}