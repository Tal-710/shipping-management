// InventoryCheckResponse.java
package com.shippingmanagement.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheckResponse {
    private boolean available;
}
