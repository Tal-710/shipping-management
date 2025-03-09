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
public class OrderStatusResponse {
    private Integer id;
    private Integer orderId;
    private String customerId;
    private Integer statusCode;
    private String status;
    private LocalDateTime createdAt;
}