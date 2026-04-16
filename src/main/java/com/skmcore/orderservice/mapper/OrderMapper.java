package com.skmcore.orderservice.mapper;

import com.skmcore.orderservice.dto.AddressRequest;
import com.skmcore.orderservice.dto.AddressResponse;
import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
import com.skmcore.orderservice.dto.OrderItemResponse;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderItem;
import com.skmcore.orderservice.model.ShippingAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // CreateOrderRequest → Order
    // customer, items, status, totalAmount, and audit/version fields are set by the service layer
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(CreateOrderRequest request);

    // AddressRequest → ShippingAddress (used implicitly by toEntity above)
    ShippingAddress toShippingAddress(AddressRequest request);

    // ShippingAddress → AddressResponse (used implicitly by toResponse below)
    AddressResponse toAddressResponse(ShippingAddress address);

    // Order → OrderResponse
    @Mapping(target = "customerEmail", source = "customer.email")
    @Mapping(target = "customerName", source = "customer.fullName")
    OrderResponse toResponse(Order order);

    // OrderItem → OrderItemResponse
    OrderItemResponse toItemResponse(OrderItem item);

    // List<OrderItem> → List<OrderItemResponse>
    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);

    // OrderItemRequest → OrderItem (used by service when attaching items to an order)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    OrderItem toItemEntity(OrderItemRequest request);
}
