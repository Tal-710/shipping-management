package com.shippingmanagement.order_service.service;

import com.shippingmanagement.order_service.dto.*;
import com.shippingmanagement.order_service.exception.InventoryNotAvailableException;
import com.shippingmanagement.order_service.mapper.OrderMapper;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;

    @Value("${app.inventory-service-url}")
    private String inventoryServiceUrl;

    @Value("${app.kafka.topics.order-submitted}")
    private String orderSubmittedTopic;

    @Transactional
    public OrderDTO createOrder(OrderRequest orderRequest) {
        log.info("Creating new order with {} items", orderRequest.getOrderItems().size());

        // Check inventory
        if (!checkInventory(orderRequest)) {
            throw new InventoryNotAvailableException("Inventory not available for one or more products");
        }

        // Create order
        Order order = Order.builder()
                .customerId(orderRequest.getCustomerId())
                .destinationCountry(orderRequest.getDestinationCountry())
                .createdAt(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();

        orderRequest.getOrderItems().forEach(itemRequest -> {
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .build();
            order.addOrderItem(orderItem);
        });

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved to database with ID: {}", savedOrder.getOrderId());

        // Publish to Kafka
        publishOrderSubmitted(savedOrder);

        return orderMapper.mapToDto(savedOrder);
    }

    private boolean checkInventory(OrderRequest orderRequest) {
        List<InventoryItemRequest> items = orderRequest.getOrderItems().stream()
                .map(item -> InventoryItemRequest.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        InventoryCheckRequest request = InventoryCheckRequest.builder()
                .items(items)
                .reserve(true)
                .build();

        try {
            InventoryCheckResponse response = webClientBuilder.build()
                    .post()
                    .uri(inventoryServiceUrl + "/api/inventory/check")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(InventoryCheckResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                log.error("Inventory check returned null response");
                throw new InventoryNotAvailableException("No response from inventory service");
            }

            if (!response.isAvailable()) {
                log.error("Inventory not available for items: {}", items);
                throw new InventoryNotAvailableException("One or more products are not available in the inventory");
            }

            return true;
        } catch (Exception e) {
            log.error("Error checking inventory", e);
            throw new InventoryNotAvailableException("Error checking inventory: " + e.getMessage());
        }
    }

    private void publishOrderSubmitted(Order order) {
        // Use existing mapper to convert Order to OrderResponse
        OrderDTO orderDTO = orderMapper.mapToDto(order);

        // Send the OrderResponse to Kafka
        kafkaTemplate.send(orderSubmittedTopic, String.valueOf(order.getOrderId()), orderDTO);
        log.info("Published order submitted event for order ID: {}", order.getOrderId());
    }
}
