package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.dto.PagedResponse;
import com.skmcore.orderservice.dto.UpdateOrderStatusRequest;
import com.skmcore.orderservice.event.OrderCreatedEvent;
import com.skmcore.orderservice.exception.EntityNotFoundException;
import com.skmcore.orderservice.exception.InvalidOrderStateException;
import com.skmcore.orderservice.exception.OrderAlreadyCancelledException;
import com.skmcore.orderservice.mapper.OrderMapper;
import com.skmcore.orderservice.model.Customer;
import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderItem;
import com.skmcore.orderservice.model.OrderStatus;
import com.skmcore.orderservice.repository.CustomerRepository;
import com.skmcore.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(OrderRepository orderRepository,
                             CustomerRepository customerRepository,
                             OrderMapper orderMapper,
                             ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.orderMapper = orderMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.customerId());

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer", request.customerId()));

        Order order = orderMapper.toEntity(request);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CREATED);

        request.items().forEach(itemRequest -> {
            OrderItem item = orderMapper.toItemEntity(itemRequest);
            order.addItem(item);
        });

        BigDecimal total = order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        log.info("Order created: {} for customer: {}", saved.getOrderNumber(), saved.getCustomer().getId());

        eventPublisher.publishEvent(
                OrderCreatedEvent.of(saved.getId(), saved.getOrderNumber(),
                        saved.getCustomer().getId(), saved.getTotalAmount()));

        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public OrderResponse getOrderById(UUID id) {
        return orderMapper.toResponse(findOrderById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        return orderMapper.toResponse(findOrderByOrderNumber(orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrders(UUID customerId, OrderStatus status, Pageable pageable) {
        Page<Order> page;
        if (customerId != null && status != null) {
            page = orderRepository.findByCustomer_IdAndStatus(customerId, status, pageable);
        } else if (customerId != null) {
            page = orderRepository.findByCustomerId(customerId, pageable);
        } else if (status != null) {
            page = orderRepository.findByStatus(status, pageable);
        } else {
            page = orderRepository.findAll(pageable);
        }
        return new PagedResponse<>(
                page.getContent().stream().map(orderMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(UUID customerId) {
        return orderRepository.findByCustomer_Id(customerId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public OrderResponse updateOrderStatus(String orderNumber, UpdateOrderStatusRequest request) {
        Order order = findOrderByOrderNumber(orderNumber);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException(order.getId());
        }

        log.info("Transitioning order {} from {} to {}", orderNumber, order.getStatus(), request.status());
        order.transitionTo(request.status());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public void cancelOrder(String orderNumber) {
        Order order = findOrderByOrderNumber(orderNumber);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException(order.getId());
        }

        try {
            order.cancel();
            orderRepository.save(order);
            log.info("Order {} cancelled", orderNumber);
        } catch (IllegalStateException ex) {
            throw new InvalidOrderStateException(order.getId(), order.getStatus(), OrderStatus.CANCELLED);
        }
    }

    private Order findOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order", id));
    }

    private Order findOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderNumber));
    }
}
