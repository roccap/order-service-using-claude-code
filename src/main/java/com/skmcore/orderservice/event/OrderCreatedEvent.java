package com.skmcore.orderservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Emitted after an order is successfully persisted.
 * Listeners can use this to send notifications, update inventory, etc.
 */
public record OrderCreatedEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        BigDecimal totalAmount,
        LocalDateTime occurredAt
) {
    public static OrderCreatedEvent of(UUID orderId, String orderNumber, UUID customerId, BigDecimal totalAmount) {
        return new OrderCreatedEvent(orderId, orderNumber, customerId, totalAmount, LocalDateTime.now());
    }
}
