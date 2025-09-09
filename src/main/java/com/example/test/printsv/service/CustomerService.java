package com.example.test.printsv.service;

import com.example.test.printsv.entity.Customer;
import com.example.test.printsv.repository.CustomerRepository;
import com.example.test.printsv.request.CustomerRequest;
import com.example.test.printsv.response.CustomerResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final UserService userService;
    private final ZakazService zakazService;


    @PostConstruct
    public void customerInit() {
        log.info("CustomerService initialized successfully.");
    }


    public CustomerResponse createCustomer(CustomerRequest customerRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        //if (userService.hasRole(username, "MANAGER") || userService.hasRole(username, "ADMIN")) {
        Customer customer = new Customer();
        customer.setName(customerRequest.getName());
        customer.setPhone(customerRequest.getPhone());
        customer = customerRepository.save(customer);
        log.info("Создан клиент: {}", customer.getName());
        CustomerResponse customerResponse = mapToCustomerResponse(customer);
        return customerResponse;

        //}
        //throw new SecurityException("Требуется роль MANAGER или ADMIN");
    }


    public CustomerResponse getCustomerByName(String name) {

        Customer customer = customerRepository.findByNameContaining(name);

        return mapToCustomerResponse(customer);
    }


    public List<CustomerResponse> getAllCustomers() {

        List<Customer> customers = customerRepository.findAll();
        return customers.stream()
                .map(this::mapToCustomerResponse)
                .collect(Collectors.toList());
    }


    public CustomerResponse updateCustomer(CustomerRequest customerRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        //if (userService.hasRole(username) || userService.hasRole(username, "ADMIN")) {
        Customer existingCustomer = customerRepository.findByNameContaining(username);

        existingCustomer.setName(customerRequest.getName());
        existingCustomer.setPhone(customerRequest.getPhone());
        customerRepository.save(existingCustomer);
        log.info("Обновлен клиент: {}", existingCustomer.getName());
        CustomerResponse customerResponse = mapToCustomerResponse(existingCustomer);
        return customerResponse;
        // }
        // throw new SecurityException("Требуется роль MANAGER или ADMIN");

    }


    public ResponseEntity<Void> deleteCustomer(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        //  if (userService.hasRole(username, "MANAGER") || userService.hasRole(username, "ADMIN")) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Клиент с id " + id + " не найден"));
        customerRepository.delete(customer);
        log.info("Удален клиент: {}", customer.getName());
        return ResponseEntity.noContent().build();
        //  }
        //    throw new SecurityException("Требуется роль MANAGER или ADMIN");
    }

    private CustomerResponse mapToCustomerResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setPhone(customer.getPhone());
        return response;
    }

    @PreDestroy
    public void customerDestroy() {
        log.info("Custromer bean destroyed");
    }

}