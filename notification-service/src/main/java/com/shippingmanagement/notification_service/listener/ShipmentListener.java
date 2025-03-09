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
import java.lang.reflect.Method;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OrderStatusService orderStatusService;
    private final StatusCodeMapper statusCodeMapper;

    @Value("${app.kafka.topics.order-status}")
    private String orderStatusTopic;

    @KafkaListener(topics = "${app.kafka.topics.shipment-created}")
    public void handleShipmentCreated(ConsumerRecord<String, Object> record) {
        try {

            Object shipment = record.value();


            int orderId = getIntValue(shipment, "getOrderId");
            String customerId = getStringValue(shipment, "getCustomerId");
            String destination = getStringValue(shipment, "getDestinationCountry");
            int shipId = getIntValue(shipment, "getShipId");

            log.info("Received shipment created: order={}, ship={}", orderId, shipId);


            OrderStatusEvent event = OrderStatusEvent.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .status(OrderStatus.SHIPPED_SUCCESSFUL)
                    .message("Order assigned to ship " + shipId)
                    .destination(destination)
                    .shipId(shipId)
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

            com.shippingmanagement.notification_service.model.OrderStatus savedStatus =
                    com.shippingmanagement.notification_service.model.OrderStatus.builder()
                            .orderId(orderId)
                            .customerId(customerId)
                            .statusCode(statusCodeMapper.getStatusId(event.getStatus()))
                            .createdAt(LocalDateTime.now())
                            .build();

            orderStatusService.saveOrderStatus(savedStatus);


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
                        .status(OrderStatus.NO_SHIP_AVAILABLE_DLT)
                        .message("Message processing failed and sent to DLT")
                        .destination("unknown")
                        .shipId(0)
                        .timestamp(LocalDateTime.now())
                        .build();


                com.shippingmanagement.notification_service.model.OrderStatus savedStatus =
                        com.shippingmanagement.notification_service.model.OrderStatus.builder()
                                .orderId(event.getOrderId())
                                .customerId("unknown")
                                .statusCode(statusCodeMapper.getStatusId(event.getStatus()))
                                .createdAt(LocalDateTime.now())
                                .build();

                orderStatusService.saveOrderStatus(savedStatus);


                String json = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(orderStatusTopic, key != null ? key : "0", json);
                return;
            }


            OrderStatusEvent event = OrderStatusEvent.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .status(OrderStatus.NO_SHIP_AVAILABLE_DLT)
                    .message("Shipping assignment failed and sent to DLT")
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
            kafkaTemplate.send(orderStatusTopic, String.valueOf(orderId), json);

        } catch (Exception e) {
            log.error("Error handling failed shipment", e);
        }
    }

    private int getIntValue(Object obj, String methodName) throws Exception {
        Method method = obj.getClass().getMethod(methodName);
        Object result = method.invoke(obj);
        return result != null ? Integer.parseInt(result.toString()) : 0;
    }

    private String getStringValue(Object obj, String methodName) throws Exception {
        Method method = obj.getClass().getMethod(methodName);
        Object result = method.invoke(obj);
        return result != null ? result.toString() : "";
    }
}