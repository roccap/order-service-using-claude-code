package com.skmcore.orderservice.repository;

import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void save_persistsOrderWithGeneratedId() {
        Order order = Order.builder()
                .orderNumber("ORD-IT-001")
                .customerId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("49.99"))
                .build();

        Order saved = orderRepository.save(order);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderNumber()).isEqualTo("ORD-IT-001");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findByCustomerId_returnsOnlyMatchingCustomerOrders() {
        UUID targetCustomer = UUID.randomUUID();
        UUID otherCustomer = UUID.randomUUID();

        orderRepository.saveAll(List.of(
                orderFor(targetCustomer, "ORD-IT-002", OrderStatus.PENDING),
                orderFor(targetCustomer, "ORD-IT-003", OrderStatus.CONFIRMED),
                orderFor(otherCustomer, "ORD-IT-004", OrderStatus.PENDING)
        ));

        List<Order> results = orderRepository.findByCustomerId(targetCustomer);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Order::getCustomerId).containsOnly(targetCustomer);
    }

    @Test
    void findByStatus_returnsOrdersWithMatchingStatus() {
        UUID customer = UUID.randomUUID();
        orderRepository.saveAll(List.of(
                orderFor(customer, "ORD-IT-005", OrderStatus.SHIPPED),
                orderFor(customer, "ORD-IT-006", OrderStatus.SHIPPED),
                orderFor(customer, "ORD-IT-007", OrderStatus.DELIVERED)
        ));

        List<Order> shipped = orderRepository.findByStatus(OrderStatus.SHIPPED);

        assertThat(shipped).hasSizeGreaterThanOrEqualTo(2);
        assertThat(shipped).extracting(Order::getStatus).containsOnly(OrderStatus.SHIPPED);
    }

    private Order orderFor(UUID customerId, String orderNumber, OrderStatus status) {
        return Order.builder()
                .orderNumber(orderNumber)
                .customerId(customerId)
                .status(status)
                .totalAmount(new BigDecimal("100.00"))
                .build();
    }
}
