package com.shippingmanagement.notification_service.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
