package com.shippingmanagement.notification_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shippingmanagement.notification_service.dto.OrderStatus;
import com.shippingmanagement.notification_service.dto.OrderStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.order-status}")
    private String orderStatusTopic;

    public void notifyShipmentSuccess(int orderId, String customerId, String destination, int shipId) {
        OrderStatusEvent event = OrderStatusEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .status(OrderStatus.SHIPPED_SUCCESSFUL)
                .message("Order has been successfully assigned to ship " + shipId)
                .destination(destination)
                .shipId(shipId)
                .timestamp(LocalDateTime.now())
                .build();

        sendNotification(String.valueOf(orderId), event);
    }

    public void notifyNoShipAvailable(int orderId, String customerId, String destination) {
        OrderStatusEvent event = OrderStatusEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .status(OrderStatus.NO_SHIP_AVAILABLE)
                .message("No ship available for destination. Will retry automatically.")
                .destination(destination)
                .shipId(0)
                .timestamp(LocalDateTime.now())
                .build();

        sendNotification(String.valueOf(orderId), event);
    }

    public void notifyOrderFailed(int orderId, String customerId, String destination) {
        OrderStatusEvent event = OrderStatusEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .status(OrderStatus.NO_SHIP_AVAILABLE_DLT)
                .message("All automatic retries failed. Order requires manual processing.")
                .destination(destination)
                .shipId(0)
                .timestamp(LocalDateTime.now())
                .build();

        sendNotification(String.valueOf(orderId), event);
    }

    private void sendNotification(String key, OrderStatusEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(orderStatusTopic, key, eventJson);
            log.info("Sent notification for order {}: status={}", event.getOrderId(), event.getStatus());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification event for order {}", event.getOrderId(), e);
        }
    }
}
