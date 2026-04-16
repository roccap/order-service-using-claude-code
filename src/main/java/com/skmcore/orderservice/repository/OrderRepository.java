package com.skmcore.orderservice.repository;

import com.skmcore.orderservice.dto.OrderStatusCount;
import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    // Used by the service layer (non-paginated)
    List<Order> findByCustomer_Id(UUID customerId);

    Page<Order> findByCustomer_IdAndStatus(UUID customerId, OrderStatus status, Pageable pageable);

    // Paginated variant — uses @Query to navigate the ManyToOne association explicitly
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId")
    Page<Order> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    // Used by the service layer (non-paginated)
    List<Order> findByStatus(OrderStatus status);

    // Paginated variant
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // Returns the count of orders grouped by status for a single customer
    @Query("SELECT new com.skmcore.orderservice.dto.OrderStatusCount(o.status, COUNT(o)) " +
           "FROM Order o WHERE o.customer.id = :customerId GROUP BY o.status")
    List<OrderStatusCount> countByStatusForCustomer(@Param("customerId") UUID customerId);
}
