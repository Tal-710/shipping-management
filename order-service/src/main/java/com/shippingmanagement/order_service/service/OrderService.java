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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
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


    public void createOrder(OrderRequest orderRequest) {
        log.info("Creating new order with {} items", orderRequest.getOrderItems().size());
        Order order = buildOrder(orderRequest);
        log.info("Order had been generated:");
        try {
            attemptOrderPlacement(order);


        } catch (InventoryNotAvailableException e) {
            handleInventoryFailure(order, e.getMessage());
            throw e;
        }
    }

    private boolean checkInventory(List<OrderItemRequestDto> orderItems) {
        InventoryResponse[] inventoryResponses = webClientBuilder.build()
                .post()
                .uri(inventoryServiceUrl + "/api/inventory/check")
                .bodyValue(orderItems)
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        log.info("Posted");
        isResponseValid(inventoryResponses);
        log.info("Inventory check successful");

        return Arrays.stream(inventoryResponses)
                .allMatch(InventoryResponse::isInStock);
    }

    private void isResponseValid(InventoryResponse[] inventoryResponses) {
        if (inventoryResponses == null) {
            log.error("Inventory check returned null response");
            throw new InventoryNotAvailableException("No response from inventory service");
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
        } else {
            log.info("Not all products are in stock");
        }
    }


    private void publishOrderSubmitted(Order order) {
        OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent(
                order.getOrderId(),
                order.getCustomerId(),
                order.getDestinationCountry(),
                order.getCreatedAt(),
                order.getOrderItems().stream()
                        .map(item -> new OrderItemRequestDto(item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList())
        );
        kafkaTemplate.send(orderSubmittedTopic, String.valueOf(order.getOrderId()), orderPlacedEvent);
        log.info("Published order submitted event for order ID: {}", order.getOrderId());
    }

    private void handleInventoryFailure(Order order, String reason) {
        log.warn("Inventory check failed for order ID: {}. Retrying via Kafka topic.", order.getOrderId());
        publishOrderInventoryUnavailable(order, reason);
    }



    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 5000, multiplier = 2.0),
            dltTopicSuffix = "-dlt")
    @KafkaListener(topics="order-inventory-retry", groupId = "order-group")
    private void publishOrderInventoryUnavailable(Order order, String reason) {
        OrderPlacedEvent orderRetryEvent = new OrderPlacedEvent(
                order.getOrderId(),
                order.getCustomerId(),
                order.getDestinationCountry(),
                order.getCreatedAt(),
                order.getOrderItems().stream()
                        .map(item -> new OrderItemRequestDto(item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList())
        );
        kafkaTemplate.send("order-inventory-retry", String.valueOf(order.getOrderId()), orderRetryEvent);
    }

    private void saveOrder(Order order) {
        orderRepository.save(order);
        log.info("Order saved to database with ID: {}", order.getOrderId());
    }
}