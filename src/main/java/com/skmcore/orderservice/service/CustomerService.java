package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.CreateCustomerRequest;
import com.skmcore.orderservice.dto.CustomerResponse;

import java.util.UUID;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse getCustomerById(UUID id);

    CustomerResponse getCustomerByEmail(String email);
}
