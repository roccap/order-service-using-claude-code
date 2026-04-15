package com.skmcore.orderservice.exception;

import com.skmcore.orderservice.model.OrderStatus;

import java.util.UUID;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(UUID orderId, OrderStatus current, OrderStatus target) {
        super(String.format("Cannot transition order %s from %s to %s", orderId, current, target));
    }
}
