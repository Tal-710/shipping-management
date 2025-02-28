package com.shippingmanagement.order_service.service;


import com.shippingmanagement.order_service.dto.OrderRequest;
import com.shippingmanagement.order_service.dto.OrderResponse;
import com.shippingmanagement.order_service.mapper.OrderMapper;
import com.shippingmanagement.order_service.model.Order;
import com.shippingmanagement.order_service.model.OrderItem;
import com.shippingmanagement.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        log.info("Creating new order with {} items", orderRequest.getOrderItems().size());

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

        return orderMapper.mapToDto(savedOrder);
    }
}
