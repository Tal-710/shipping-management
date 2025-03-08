package com.shippingmanagement.order_service.service;

import com.shippingmanagement.order_service.dto.*;
import com.shippingmanagement.order_service.event.OrderPlacedEvent;
import com.shippingmanagement.order_service.exception.InventoryNotAvailableException;
import com.shippingmanagement.order_service.model.Order;
import com.shippingmanagement.order_service.model.OrderItem;
import com.shippingmanagement.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Value("${app.inventory-service-url}")
    private String inventoryServiceUrl;

    @Value("${app.kafka.topics.order-submitted}")
    private String orderSubmittedTopic;

    @Value("${app.kafka.topics.order-inventory-dlt}")
    private String orderDltTopic;


    public void createOrder(OrderRequest orderRequest) {
        log.info("Creating new order with {} items", orderRequest.getOrderItems().size());
        Order order = buildOrder(orderRequest);
        log.info("Order had been generated: {} ", order.getOrderId());
        try {
            attemptOrderPlacement(order);
        } catch (InventoryNotAvailableException e) {
            log.warn("Inventory failure for order {}: {}", order.getOrderId(), e.getMessage());
            handleInventoryFailure(order, e.getMessage());
        }
    }

    private boolean checkInventory(List<OrderItemRequestDto> orderItems) {
        try {
            InventoryResponse[] inventoryResponses = webClientBuilder.build()
                    .post()
                    .uri(inventoryServiceUrl + "/api/inventory/check")
                    .bodyValue(orderItems)
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            if (inventoryResponses == null) {
                throw new InventoryNotAvailableException("No response from inventory service");
            }

            boolean allAvailable = Arrays.stream(inventoryResponses)
                    .allMatch(InventoryResponse::isInStock);

            if (!allAvailable) {
                log.warn("Stock unavailable for order.");
                throw new InventoryNotAvailableException("Inventory out of quantity for certain product");

            }

            return true;

        } catch (Exception e) {
            log.error("Failed to check inventory: {}", e.getMessage());
            throw new InventoryNotAvailableException("Inventory service unavailable");
        }
    }


    private Order buildOrder(OrderRequest orderRequest) {
        Order order = Order.builder()
                .customerId(orderRequest.getCustomerId())
                .destinationCountry(orderRequest.getDestinationCountry())
                .createdAt(LocalDateTime.now())
                .orderItems(orderRequest.getOrderItems().stream()
                        .map(this::buildOrderItem)
                        .collect(Collectors.toList()))
                .build();

        order.getOrderItems().forEach(orderItem -> orderItem.setOrder(order));

        return order;
    }

    private OrderItem buildOrderItem(OrderItemRequestDto itemRequest) {
        return OrderItem.builder()
                .productId(itemRequest.getProductId())
                .quantity(itemRequest.getQuantity())
                .build();
    }


    private void attemptOrderPlacement(Order order) {
        List<OrderItemRequestDto> orderItemsDto = order.getOrderItems().stream()
                .map(item -> new OrderItemRequestDto(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());

        log.info("Attempting to submit order item {}", orderItemsDto);

        if (checkInventory(orderItemsDto)) {
            log.info("All products available");
            saveOrder(order);
            publishOrderSubmitted(order);
        }
    }


    private void publishOrderSubmitted(Order order) {
        OrderPlacedEvent orderPlacedEvent = buildOrderPlacedEvent(order);
        kafkaTemplate.send(orderSubmittedTopic, String.valueOf(order.getOrderId()), orderPlacedEvent);
        log.info("Published order submitted event for order ID: {}", order.getOrderId());
    }

    private void handleInventoryFailure(Order order, String reason) {
        log.warn("Order {} has failed", order.getOrderId());
        kafkaTemplate.send(orderDltTopic, String.valueOf(order.getOrderId()),  buildOrderPlacedEvent(order));
    }

    private OrderPlacedEvent buildOrderPlacedEvent(Order order) {
        return new OrderPlacedEvent(
                order.getOrderId(),
                order.getCustomerId(),
                order.getDestinationCountry(),
                order.getCreatedAt(),
                order.getOrderItems().stream()
                        .map(item -> new OrderItemRequestDto(item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList())
        );
    }

    private void saveOrder(Order order) {
        orderRepository.save(order);
        log.info("Order saved to database with ID: {}", order.getOrderId());
    }
}