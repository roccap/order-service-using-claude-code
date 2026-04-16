package com.skmcore.orderservice.dto;

import com.skmcore.orderservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        String customerEmail,
        String customerName,
        List<OrderItemResponse> items,
        AddressResponse shippingAddress,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
