package com.shippingmanagement.notification_service.controller;

import com.shippingmanagement.notification_service.dto.OrderStatusResponse;
import com.shippingmanagement.notification_service.service.OrderStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-status")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OrderStatusController {

    private final OrderStatusService orderStatusService;

    @GetMapping("/all")
    public ResponseEntity<List<OrderStatusResponse>> getAllOrderStatuses() {
        log.info("Request received to get all order statuses");
        List<OrderStatusResponse> responses = orderStatusService.getAllOrderStatusesWithDetails();
        return ResponseEntity.ok(responses);
    }
}