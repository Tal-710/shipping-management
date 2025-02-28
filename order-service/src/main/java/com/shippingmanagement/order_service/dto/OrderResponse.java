package com.shippingmanagement.order_service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Integer orderId;
    private String customerId;
    private String destinationCountry;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> orderItems;
}
