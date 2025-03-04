package com.shippingmanagement.order_service.mapper;

import com.shippingmanagement.order_service.dto.OrderItemResponse;
import com.shippingmanagement.order_service.dto.OrderDTO;
import com.shippingmanagement.order_service.model.Order;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderDTO mapToDto(Order order) {
        return OrderDTO.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .destinationCountry(order.getDestinationCountry())
                .createdAt(order.getCreatedAt())
                .orderItems(order.getOrderItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
