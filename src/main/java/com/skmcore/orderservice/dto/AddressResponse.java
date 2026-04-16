package com.skmcore.orderservice.dto;

public record AddressResponse(
        String street,
        String city,
        String state,
        String zipCode,
        String country
) {}
