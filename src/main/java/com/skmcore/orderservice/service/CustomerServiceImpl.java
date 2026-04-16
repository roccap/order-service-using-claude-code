package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.CreateCustomerRequest;
import com.skmcore.orderservice.dto.CustomerResponse;
import com.skmcore.orderservice.exception.DuplicateEmailException;
import com.skmcore.orderservice.exception.EntityNotFoundException;
import com.skmcore.orderservice.mapper.CustomerMapper;
import com.skmcore.orderservice.model.Customer;
import com.skmcore.orderservice.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        Customer customer = customerRepository.save(customerMapper.toEntity(request));
        log.info("Customer created: id={} email={}", customer.getId(), customer.getEmail());
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id) {
        return customerMapper.toResponse(
                customerRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Customer", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        return customerMapper.toResponse(
                customerRepository.findByEmail(email)
                        .orElseThrow(() -> new EntityNotFoundException("Customer", email)));
    }
}
