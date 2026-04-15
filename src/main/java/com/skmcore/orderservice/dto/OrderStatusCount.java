package com.skmcore.orderservice.dto;

import com.skmcore.orderservice.model.OrderStatus;

public record OrderStatusCount(OrderStatus status, Long count) {}
