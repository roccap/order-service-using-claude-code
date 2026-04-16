package com.skmcore.orderservice.controller;

import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.dto.PagedResponse;
import com.skmcore.orderservice.dto.UpdateOrderStatusRequest;
import com.skmcore.orderservice.model.OrderStatus;
import com.skmcore.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order for an existing customer. Items and shipping address are required.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or validation failure"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.debug("POST /api/v1/orders - customerId={}", request.customerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get order by order number", description = "Returns a single order identified by its order number (e.g. ORD-AB12CD34).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order number", example = "ORD-AB12CD34")
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByOrderNumber(orderNumber));
    }

    @GetMapping
    @Operation(
            summary = "List orders with pagination",
            description = "Returns a paginated list of orders. Optionally filter by customer ID or status. " +
                          "Supports page, size, and sort query parameters."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of orders returned")
    })
    public ResponseEntity<PagedResponse<OrderResponse>> getOrders(
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) UUID customerId,
            @Parameter(description = "Filter by order status")
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrders(customerId, status, pageable));
    }

    @PatchMapping("/{orderNumber}/status")
    @Operation(summary = "Update order status", description = "Transitions an order to a new status. Allowed transitions: CREATED→CONFIRMED/CANCELLED, CONFIRMED→SHIPPED/CANCELLED, SHIPPED→DELIVERED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Order is already cancelled"),
            @ApiResponse(responseCode = "422", description = "Invalid status transition")
    })
    public ResponseEntity<OrderResponse> updateStatus(
            @Parameter(description = "Order number", example = "ORD-AB12CD34")
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderNumber, request));
    }

    @DeleteMapping("/{orderNumber}")
    @Operation(summary = "Cancel an order", description = "Soft-cancels an order by transitioning it to CANCELLED status. Only CREATED and CONFIRMED orders can be cancelled.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Order is already cancelled"),
            @ApiResponse(responseCode = "422", description = "Order cannot be cancelled in its current state")
    })
    public ResponseEntity<Void> cancelOrder(
            @Parameter(description = "Order number", example = "ORD-AB12CD34")
            @PathVariable String orderNumber) {
        orderService.cancelOrder(orderNumber);
        return ResponseEntity.noContent().build();
    }
}
