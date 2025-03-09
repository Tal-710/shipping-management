package com.shippingmanagement.notification_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shippingmanagement.notification_service.dto.OrderStatus;
import com.shippingmanagement.notification_service.dto.OrderStatusEvent;
import com.shippingmanagement.notification_service.service.OrderStatusService;
import com.shippingmanagement.notification_service.util.StatusCodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OrderStatusService orderStatusService;
    private final StatusCodeMapper statusCodeMapper;

    @Value("${app.kafka.topics.order-status}")
    private String orderStatusTopic;

    @KafkaListener(topics = "order-submitted", containerFactory = "jsonKafkaListenerContainerFactory")
    public void handleOrderSubmitted(ConsumerRecord<String, String> record) {
        try {
            String jsonValue = record.value();
            log.info("Received order submitted: {}", jsonValue);

            String orderIdStr = record.key();
            int orderId = Integer.parseInt(orderIdStr);

            String customerId = "unknown";
            String destination = "";
            try {
                Map<String, Object> orderMap = objectMapper.readValue(jsonValue, Map.class);
                customerId = orderMap.get("customerId") != null ? orderMap.get("customerId").toString() : "unknown";
                destination = orderMap.get("destinationCountry") != null ? orderMap.get("destinationCountry").toString() : "";
            } catch (Exception e) {
                log.warn("Could not parse order JSON for metadata: {}", e.getMessage());
            }

            log.info("Processing submitted order: orderId={}, customerId={}, destination={}", orderId, customerId, destination);


            OrderStatusEvent event = OrderStatusEvent.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .status(OrderStatus.ORDER_PROCESS)
                    .message("Order is being processed")
                    .destination(destination)
                    .shipId(0)
                    .timestamp(LocalDateTime.now())
                    .build();


            com.shippingmanagement.notification_service.model.OrderStatus savedStatus =
                    com.shippingmanagement.notification_service.model.OrderStatus.builder()
                            .orderId(orderId)
                            .customerId(customerId)
                            .statusCode(statusCodeMapper.getStatusId(event.getStatus()))
                            .createdAt(LocalDateTime.now())
                            .build();

            orderStatusService.saveOrderStatus(savedStatus);


            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(orderStatusTopic, orderIdStr, json);

        } catch (Exception e) {
            log.error("Error processing order submitted", e);
        }
    }

    @KafkaListener(topics = "order-inventory-dlt", containerFactory = "jsonKafkaListenerContainerFactory")
    public void handleInventoryFailure(ConsumerRecord<String, String> record) {
        try {
            String jsonValue = record.value();
            log.info("Received inventory failure: {}", jsonValue);

            Map<String, Object> orderMap = objectMapper.readValue(jsonValue, Map.class);
            String customerId = orderMap.get("customerId") != null ? orderMap.get("customerId").toString() : "unknown";
            String destination = orderMap.get("destinationCountry") != null ? orderMap.get("destinationCountry").toString() : "unknown";


            int negativeOrderId = orderStatusService.getNextNegativeOrderId();
            log.info("Using negative order ID: {}", negativeOrderId);

            OrderStatusEvent event = OrderStatusEvent.builder()
                    .orderId(negativeOrderId)
                    .customerId(customerId)
                    .status(OrderStatus.ORDER_FAILED)
                    .message("Inventory check failed - Items not available")
                    .destination(destination)
                    .shipId(0)
                    .timestamp(LocalDateTime.now())
                    .build();


            com.shippingmanagement.notification_service.model.OrderStatus savedStatus =
                    com.shippingmanagement.notification_service.model.OrderStatus.builder()
                            .orderId(negativeOrderId)
                            .customerId(customerId)
                            .statusCode(statusCodeMapper.getStatusId(event.getStatus()))
                            .createdAt(LocalDateTime.now())
                            .build();

            orderStatusService.saveOrderStatus(savedStatus);


            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(orderStatusTopic, String.valueOf(negativeOrderId), json);

            log.info("Sent failed order with ID: {}", negativeOrderId);
        } catch (Exception e) {
            log.error("Error handling inventory failure", e);
        }
    }
}