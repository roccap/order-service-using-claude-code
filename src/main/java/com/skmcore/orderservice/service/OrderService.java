package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.dto.PagedResponse;
import com.skmcore.orderservice.dto.UpdateOrderStatusRequest;
import com.skmcore.orderservice.model.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(UUID id);

    OrderResponse getOrderByOrderNumber(String orderNumber);

    PagedResponse<OrderResponse> getOrders(UUID customerId, OrderStatus status, Pageable pageable);

    List<OrderResponse> getOrdersByCustomerId(UUID customerId);

    List<OrderResponse> getOrdersByStatus(OrderStatus status);

    OrderResponse updateOrderStatus(String orderNumber, UpdateOrderStatusRequest request);

    void cancelOrder(String orderNumber);
}
