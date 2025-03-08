package com.shippingmanagement.notification_service.service;

import com.shippingmanagement.notification_service.dto.OrderStatusResponse;
import com.shippingmanagement.notification_service.model.OrderStatus;
import com.shippingmanagement.notification_service.repository.OrderStatusRepository;
import com.shippingmanagement.notification_service.util.StatusCodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusService {

    private final OrderStatusRepository orderStatusRepository;
    private final StatusCodeMapper statusCodeMapper;

    private final AtomicInteger negativeOrderIdCounter = new AtomicInteger(-1);

    private boolean counterInitialized = false;

    @PostConstruct
    public void init() {
        initializeNegativeOrderIdCounter();
    }

    private synchronized void initializeNegativeOrderIdCounter() {
        if (!counterInitialized) {
            Integer smallestNegativeId = orderStatusRepository.findSmallestOrderId();
            log.info("Query result for smallest negative ID: {}", smallestNegativeId);

            if (smallestNegativeId != null && smallestNegativeId < 0) {
                negativeOrderIdCounter.set(smallestNegativeId - 1);
                log.info("Initialized negative order ID counter to {}", negativeOrderIdCounter.get());
            } else {
                log.info("No negative order IDs found, starting counter at -1");
            }

            counterInitialized = true;
        }
    }

    @Transactional
    public void saveOrderStatus(OrderStatus status) {
        orderStatusRepository.save(status);
    }

    public int getNextNegativeOrderId() {
        if (!counterInitialized) {
            initializeNegativeOrderIdCounter();
        }
        return negativeOrderIdCounter.getAndDecrement();
    }

    public List<OrderStatusResponse> getAllOrderStatusesWithDetails() {
        log.info("Getting all order statuses from database with string representation");
        List<OrderStatus> allStatuses = orderStatusRepository.findAll();
        return allStatuses.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private OrderStatusResponse convertToResponse(OrderStatus status) {
        com.shippingmanagement.notification_service.dto.OrderStatus statusEnum =
                statusCodeMapper.getStatusFromCode(status.getStatusCode());

        return OrderStatusResponse.builder()
                .id(status.getId())
                .orderId(status.getOrderId())
                .customerId(status.getCustomerId())
                .statusCode(status.getStatusCode())
                .status(statusEnum.name())
                .createdAt(status.getCreatedAt())
                .build();
    }
}