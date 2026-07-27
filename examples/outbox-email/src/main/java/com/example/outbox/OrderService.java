package com.example.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sk.solodev.notify.email.EmailRequest;
import sk.solodev.notify.outbox.OutboxNotifier;

import java.util.UUID;

@Service
class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxNotifier outboxNotifier;

    OrderService(OrderRepository orderRepository, OutboxNotifier outboxNotifier) {
        this.orderRepository = orderRepository;
        this.outboxNotifier = outboxNotifier;
    }

    @Transactional
    UUID placeOrder(Order order, boolean shouldFail) {
        orderRepository.insert(order);

        outboxNotifier.enqueue(EmailRequest.builder()
                .to(order.customerEmail())
                .from("orders@example.com")
                .subject("Order confirmation: " + order.productName())
                .body("Thank you for your order!\n\nProduct: %s\nAmount: $%s\n\nOrder ID: %s"
                        .formatted(order.productName(), order.amount(), order.id()))
                .build());

        if (shouldFail) {
            throw new IllegalStateException("Simulated failure — transaction will roll back");
        }

        return order.id();
    }
}