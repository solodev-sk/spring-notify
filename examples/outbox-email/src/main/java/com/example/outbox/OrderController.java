package com.example.outbox;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    Map<String, UUID> createOrder(@RequestBody Order order,
                                   @RequestParam(defaultValue = "false") boolean fail) {
        var orderId = orderService.placeOrder(order, fail);
        return Map.of("orderId", orderId);
    }
}