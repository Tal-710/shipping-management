package com.shippingmanagement.notification_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shippingmanagement.notification_service.dto.OrderStatus;
import com.shippingmanagement.notification_service.dto.OrderStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.lang.reflect.Method;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.order-status}")
    private String orderStatusTopic;

    @KafkaListener(topics = "${app.kafka.topics.shipment-created}")
    public void handleShipmentCreated(ConsumerRecord<String, Object> record) {
        try {
            // Use reflection to extract data safely without class casting
            Object shipment = record.value();

            // Extract values using reflection to avoid class casting
            int orderId = getIntValue(shipment, "getOrderId");
            String customerId = getStringValue(shipment, "getCustomerId");
            String destination = getStringValue(shipment, "getDestinationCountry");
            int shipId = getIntValue(shipment, "getShipId");

            log.info("Received shipment created: order={}, ship={}", orderId, shipId);

            // Create status event
            OrderStatusEvent event = OrderStatusEvent.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .status(OrderStatus.SHIPPED_SUCCESSFUL)
                    .message("Order assigned to ship " + shipId)
                    .destination(destination)
                    .shipId(shipId)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Send to order status topic
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(orderStatusTopic, String.valueOf(orderId), json);

        } catch (Exception e) {
            log.error("Error processing shipment created", e);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.unassigned-shipping-orders}")
    public void handleUnassignedShipment(ConsumerRecord<String, Object> record) {
        try {

            Object shipment = record.value();

            int orderId = getIntValue(shipment, "getOrderId");
            String customerId = getStringValue(shipment, "getCustomerId");
            String destination = getStringValue(shipment, "getDestinationCountry");

            log.info("Received unassigned shipment: order={}", orderId);

            OrderStatusEvent event = OrderStatusEvent.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .status(OrderStatus.NO_SHIP_AVAILABLE)
                    .message("No ship available for destination")
                    .destination(destination)
                    .shipId(0)
                    .timestamp(LocalDateTime.now())
                    .build();

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(orderStatusTopic, String.valueOf(orderId), json);

        } catch (Exception e) {
            log.error("Error processing unassigned shipment", e);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.unassigned-shipping-orders-dlt}")
    public void handleFailedShipment(ConsumerRecord<String, Object> record) {
        try {
            Object shipment = record.value();

            int orderId;
            String customerId;
            String destination;

            try {
                orderId = getIntValue(shipment, "getOrderId");
                customerId = getStringValue(shipment, "getCustomerId");
                destination = getStringValue(shipment, "getDestinationCountry");
            } catch (Exception e) {
                log.warn("Could not extract fields from DLT message: {}", e.getMessage());
                String key = record.key();

                OrderStatusEvent event = OrderStatusEvent.builder()
                        .orderId(key != null ? Integer.parseInt(key) : 0)
                        .customerId("unknown")
                        .status(OrderStatus.ORDER_FAILED)
                        .message("Message processing failed and sent to DLT")
                        .destination("unknown")
                        .shipId(0)
                        .timestamp(LocalDateTime.now())
                        .build();

                String json = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(orderStatusTopic, key != null ? key : "0", json);
                return;
            }

            log.info("Received failed shipment from DLT: order={}", orderId);

            OrderStatusEvent event = OrderStatusEvent.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .status(OrderStatus.NO_SHIP_AVAILABLE_DLT)
                    .message("All retry attempts failed")
                    .destination(destination)
                    .shipId(0)
                    .timestamp(LocalDateTime.now())
                    .build();

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(orderStatusTopic, String.valueOf(orderId), json);

        } catch (Exception e) {
            log.error("Error processing failed shipment from DLT", e);
            // Last resort - create a generic failure notification
            try {
                OrderStatusEvent event = OrderStatusEvent.builder()
                        .orderId(0)
                        .customerId("unknown")
                        .status(OrderStatus.ORDER_FAILED)
                        .message("Fatal error processing failed shipment")
                        .destination("unknown")
                        .shipId(0)
                        .timestamp(LocalDateTime.now())
                        .build();

                String json = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(orderStatusTopic, "error", json);
            } catch (Exception ex) {
                log.error("Could not send error notification", ex);
            }
        }
    }

    private String getStringValue(Object obj, String methodName) {
        try {
            Method method = obj.getClass().getMethod(methodName);
            Object result = method.invoke(obj);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            log.warn("Error getting string value for method {}: {}", methodName, e.getMessage());
            return "";
        }
    }

    private int getIntValue(Object obj, String methodName) {
        try {
            Method method = obj.getClass().getMethod(methodName);
            Object result = method.invoke(obj);
            return result != null ? (Integer) result : 0;
        } catch (Exception e) {
            log.warn("Error getting int value for method {}: {}", methodName, e.getMessage());
            return 0;
        }
    }
}