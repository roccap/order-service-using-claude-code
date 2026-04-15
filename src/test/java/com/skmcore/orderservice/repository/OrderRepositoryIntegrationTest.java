package com.skmcore.orderservice.repository;

import com.skmcore.orderservice.model.Customer;
import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    private CustomerRepository customerRepository;

    private Customer savedCustomer;

    @BeforeEach
    void setUp() {
        savedCustomer = customerRepository.save(Customer.builder()
                .email("test-" + UUID.randomUUID() + "@example.com")
                .fullName("Test User")
                .build());
    }

    @Test
    void save_persistsOrderWithGeneratedId() {
        Order order = Order.builder()
                .customer(savedCustomer)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("49.99"))
                .build();

        Order saved = orderRepository.save(order);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderNumber()).startsWith("ORD-");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void findByCustomer_Id_returnsOnlyMatchingCustomerOrders() {
        Customer otherCustomer = customerRepository.save(Customer.builder()
                .email("other-" + UUID.randomUUID() + "@example.com")
                .fullName("Other User")
                .build());

        orderRepository.saveAll(List.of(
                orderFor(savedCustomer, OrderStatus.CREATED),
                orderFor(savedCustomer, OrderStatus.CONFIRMED),
                orderFor(otherCustomer, OrderStatus.CREATED)
        ));

        List<Order> results = orderRepository.findByCustomer_Id(savedCustomer.getId());

        assertThat(results).hasSize(2);
        assertThat(results).extracting(o -> o.getCustomer().getId()).containsOnly(savedCustomer.getId());
    }

    @Test
    void findByStatus_returnsOrdersWithMatchingStatus() {
        orderRepository.saveAll(List.of(
                orderFor(savedCustomer, OrderStatus.SHIPPED),
                orderFor(savedCustomer, OrderStatus.SHIPPED),
                orderFor(savedCustomer, OrderStatus.DELIVERED)
        ));

        List<Order> shipped = orderRepository.findByStatus(OrderStatus.SHIPPED);

        assertThat(shipped).hasSizeGreaterThanOrEqualTo(2);
        assertThat(shipped).extracting(Order::getStatus).containsOnly(OrderStatus.SHIPPED);
    }

    private Order orderFor(Customer customer, OrderStatus status) {
        return Order.builder()
                .customer(customer)
                .status(status)
                .totalAmount(new BigDecimal("100.00"))
                .build();
    }
}
