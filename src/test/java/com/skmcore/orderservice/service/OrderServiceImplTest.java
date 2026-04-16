package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.AddressRequest;
import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.dto.UpdateOrderStatusRequest;
import com.skmcore.orderservice.event.OrderCreatedEvent;
import com.skmcore.orderservice.exception.EntityNotFoundException;
import com.skmcore.orderservice.exception.OrderAlreadyCancelledException;
import com.skmcore.orderservice.mapper.OrderMapper;
import com.skmcore.orderservice.model.Customer;
import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderItem;
import com.skmcore.orderservice.model.OrderStatus;
import com.skmcore.orderservice.repository.CustomerRepository;
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
    private CustomerRepository customerRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID customerId;
    private UUID orderId;
    private Customer customer;
    private AddressRequest address;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        customer = Customer.builder()
                .id(customerId)
                .email("test@example.com")
                .fullName("Test User")
                .build();
        address = new AddressRequest("123 Main St", "Springfield", "IL", "62701", "US");
    }

    @Test
    void createOrder_persistsOrderAndPublishesEvent() {
        OrderItemRequest itemRequest = new OrderItemRequest(
                "PROD-001", "Widget", 2, new BigDecimal("9.99"));
        CreateOrderRequest request = new CreateOrderRequest(customerId, List.of(itemRequest), address);

        OrderItem item = OrderItem.builder()
                .productId("PROD-001")
                .productName("Widget")
                .quantity(2)
                .unitPrice(new BigDecimal("9.99"))
                .build();

        Order mappedOrder = Order.builder().build();

        Order savedOrder = Order.builder()
                .id(orderId)
                .orderNumber("ORD-ABCD1234")
                .customer(customer)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("19.98"))
                .items(List.of(item))
                .build();

        OrderResponse expectedResponse = new OrderResponse(
                orderId, "ORD-ABCD1234", OrderStatus.CREATED,
                "test@example.com", "Test User",
                List.of(), null, new BigDecimal("19.98"), null, null);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(orderMapper.toEntity(request)).thenReturn(mappedOrder);
        when(orderMapper.toItemEntity(itemRequest)).thenReturn(item);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(expectedResponse);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result.customerEmail()).isEqualTo("test@example.com");
        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.totalAmount()).isEqualByComparingTo("19.98");
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void createOrder_throwsEntityNotFoundException_whenCustomerNotFound() {
        CreateOrderRequest request = new CreateOrderRequest(customerId, List.of(
                new OrderItemRequest("PROD-001", "Widget", 1, BigDecimal.TEN)), address);

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(customerId.toString());
    }

    @Test
    void getOrderById_returnsOrder_whenFound() {
        Order order = Order.builder().id(orderId).customer(customer).status(OrderStatus.CREATED).build();
        OrderResponse expected = new OrderResponse(
                orderId, "ORD-ABCD1234", OrderStatus.CREATED,
                "test@example.com", "Test User",
                List.of(), null, BigDecimal.TEN, null, null);

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
        String orderNumber = "ORD-ABCD1234";
        Order order = Order.builder()
                .id(orderId)
                .orderNumber(orderNumber)
                .customer(customer)
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.TEN)
                .build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.cancelOrder(orderNumber);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_throwsOrderAlreadyCancelledException_whenAlreadyCancelled() {
        String orderNumber = "ORD-ABCD1234";
        Order order = Order.builder()
                .id(orderId)
                .orderNumber(orderNumber)
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderNumber))
                .isInstanceOf(OrderAlreadyCancelledException.class);
    }

    @Test
    void updateOrderStatus_transitionsStatus_whenOrderIsActive() {
        String orderNumber = "ORD-ABCD1234";
        Order order = Order.builder()
                .id(orderId)
                .orderNumber(orderNumber)
                .customer(customer)
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.TEN)
                .build();
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);
        OrderResponse expected = new OrderResponse(
                orderId, "ORD-ABCD1234", OrderStatus.CONFIRMED,
                "test@example.com", "Test User",
                List.of(), null, BigDecimal.TEN, null, null);

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expected);

        OrderResponse result = orderService.updateOrderStatus(orderNumber, request);

        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateOrderStatus_throwsOrderAlreadyCancelledException_whenCancelled() {
        String orderNumber = "ORD-ABCD1234";
        Order order = Order.builder()
                .id(orderId)
                .orderNumber(orderNumber)
                .status(OrderStatus.CANCELLED)
                .build();
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderNumber, request))
                .isInstanceOf(OrderAlreadyCancelledException.class);
    }
}
