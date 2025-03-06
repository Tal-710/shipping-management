package com.shippingmanagement.order_service.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shippingmanagement.order_service.dto.OrderItemRequestDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {
    @JsonProperty("orderId")
    private Integer orderId;
    private String customerId;
    private String destinationCountry;
    private LocalDateTime createdAt;
    private List<OrderItemRequestDto> orderItems;
}
