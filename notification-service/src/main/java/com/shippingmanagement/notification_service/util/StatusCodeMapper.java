package com.shippingmanagement.notification_service.util;

import com.shippingmanagement.notification_service.dto.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class StatusCodeMapper {

    private final Map<OrderStatus, Integer> statusToCode = new HashMap<>();
    private final Map<Integer, OrderStatus> codeToStatus = new HashMap<>();
    private final Map<String, OrderStatus> stringToStatus = new HashMap<>();

    public StatusCodeMapper() {
        statusToCode.put(OrderStatus.ORDER_RECEIVED, 1);
        statusToCode.put(OrderStatus.ORDER_PROCESS, 2);
        statusToCode.put(OrderStatus.SHIPPED_SUCCESSFUL, 3);
        statusToCode.put(OrderStatus.NO_SHIP_AVAILABLE, 4);
        statusToCode.put(OrderStatus.ORDER_FAILED, 5);
        statusToCode.put(OrderStatus.NO_SHIP_AVAILABLE_DLT, 6);

        statusToCode.forEach((status, code) -> codeToStatus.put(code, status));

        for (OrderStatus status : OrderStatus.values()) {
            stringToStatus.put(status.name(), status);
        }
    }

    public Integer getStatusId(OrderStatus status) {
        return statusToCode.getOrDefault(status, 0);
    }

    public OrderStatus getStatusFromCode(Integer code) {
        return codeToStatus.getOrDefault(code, OrderStatus.ORDER_RECEIVED);
    }
}