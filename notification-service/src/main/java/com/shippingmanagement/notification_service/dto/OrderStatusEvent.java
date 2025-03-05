package com.shippingmanagement.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusEvent {
    private Integer orderId;
    private String customerId;
    private OrderStatus status;
    private String message;
    private String destination;
    private Integer shipId;
    private LocalDateTime timestamp;
}
