package com.shippingmanagement.order_service.exception;

public class InventoryNotAvailableException extends RuntimeException {
    public InventoryNotAvailableException(String message) {
        super(message);
    }
}