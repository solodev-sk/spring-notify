package com.example.outbox;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
class OrderRepository {

    private final JdbcClient jdbc;

    OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    void insert(Order order) {
        jdbc.sql("""
                        INSERT INTO orders (id, customer_email, product_name, amount)
                        VALUES (:id, :customerEmail, :productName, :amount)
                        """)
                .param("id", order.id())
                .param("customerEmail", order.customerEmail())
                .param("productName", order.productName())
                .param("amount", order.amount())
                .update();
    }

    @Nullable Order findById(UUID id) {
        return jdbc.sql("SELECT id, customer_email, product_name, amount FROM orders WHERE id = :id")
                .param("id", id)
                .query((rs, rowNum) -> new Order(
                        (UUID) rs.getObject("id"),
                        rs.getString("customer_email"),
                        rs.getString("product_name"),
                        rs.getBigDecimal("amount")
                ))
                .optional()
                .orElse(null);
    }
}