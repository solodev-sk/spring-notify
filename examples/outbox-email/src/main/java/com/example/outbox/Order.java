package com.example.outbox;

import java.math.BigDecimal;
import java.util.UUID;

record Order(UUID id, String customerEmail, String productName, BigDecimal amount) {
    
}