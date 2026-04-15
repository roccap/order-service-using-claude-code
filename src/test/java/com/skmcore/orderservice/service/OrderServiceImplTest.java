package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.dto.UpdateOrderStatusRequest;
import com.skmcore.orderservice.event.OrderCreatedEvent;
import com.skmcore.orderservice.exception.EntityNotFoundException;
import com.skmcore.orderservice.exception.OrderAlreadyCancelledException;
import com.skmcore.orderservice.mapper.OrderMapper;
import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderItem;
import com.skmcore.orderservice.model.OrderStatus;
import com.skmcore.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID customerId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    void createOrder_persistsOrderAndPublishesEvent() {
        OrderItemRequest itemRequest = new OrderItemRequest(
                UUID.randomUUID(), "Widget", 2, new BigDecimal("9.99"));
        CreateOrderRequest request = new CreateOrderRequest(customerId, List.of(itemRequest), null);

        OrderItem item = OrderItem.builder()
                .productId(itemRequest.productId())
                .productName(itemRequest.productName())
                .quantity(itemRequest.quantity())
                .unitPrice(itemRequest.unitPrice())
                .build();

        Order savedOrder = Order.builder()
                .id(orderId)
                .orderNumber("ORD-001")
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("19.98"))
                .items(List.of(item))
                .build();

        OrderResponse expectedResponse = new OrderResponse(
                orderId, "ORD-001", customerId, OrderStatus.PENDING,
                new BigDecimal("19.98"), null, List.of(), null, null);

        when(orderMapper.toItemEntity(itemRequest)).thenReturn(item);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(expectedResponse);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result.customerId()).isEqualTo(customerId);
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.totalAmount()).isEqualByComparingTo("19.98");
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void getOrderById_returnsOrder_whenFound() {
        Order order = Order.builder().id(orderId).customerId(customerId).status(OrderStatus.PENDING).build();
        OrderResponse expected = new OrderResponse(
                orderId, "ORD-001", customerId, OrderStatus.PENDING, BigDecimal.TEN, null, List.of(), null, null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(expected);

        assertThat(orderService.getOrderById(orderId)).isEqualTo(expected);
    }

    @Test
    void getOrderById_throwsEntityNotFoundException_whenNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    void cancelOrder_setsStatusToCancelled_whenAllowed() {
        Order order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.cancelOrder(orderId);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_throwsOrderAlreadyCancelledException_whenAlreadyCancelled() {
        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                .isInstanceOf(OrderAlreadyCancelledException.class);
    }

    @Test
    void updateOrderStatus_transitionsStatus_whenOrderIsActive() {
        Order order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .build();
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, null);
        OrderResponse expected = new OrderResponse(
                orderId, "ORD-001", customerId, OrderStatus.CONFIRMED, BigDecimal.TEN, null, List.of(), null, null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expected);

        OrderResponse result = orderService.updateOrderStatus(orderId, request);

        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateOrderStatus_throwsOrderAlreadyCancelledException_whenCancelled() {
        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.CANCELLED)
                .build();
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, request))
                .isInstanceOf(OrderAlreadyCancelledException.class);
    }
}
