package com.shippingmanagement.order_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/order-status")
    public String orderStatus() {
        return "order-status";
    }

    @GetMapping("/inventory-status")
    public String inventoryStatus() {
        return "inventory-status";
    }

    @GetMapping("/shipping-status")
    public String shippingStatus() {
        return "shipping-status";
    }
}
