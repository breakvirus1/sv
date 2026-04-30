package com.example.employeeservice.service;

import com.example.common.entity.Employee;
import com.example.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public Page<Employee> getAllEmployees(Specification<Employee> spec, Pageable pageable) {
        return employeeRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
    }

    public Employee createEmployee(Employee employee) {
        if (employeeRepository.findByUsername(employee.getUsername()).isPresent()) {
            throw new RuntimeException("Сотрудник с таким логином уже существует");
        }
        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee employee = getEmployeeById(id);
        employee.setFullName(employeeDetails.getFullName());
        employee.setUsername(employeeDetails.getUsername());
        employee.setPosition(employeeDetails.getPosition());
        employee.setPhone(employeeDetails.getPhone());
        employee.setEmail(employeeDetails.getEmail());
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(Long id) {
        Employee employee = getEmployeeById(id);
        employeeRepository.delete(employee);
    }

    public Employee syncOrCreateFromKeycloak(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isEmpty()) {
            username = jwt.getSubject();
        }
        String fullName = jwt.getClaimAsString("name");
        String email = jwt.getClaimAsString("email");

        Employee employee = employeeRepository.findByUsername(username)
                .orElseGet(() -> new Employee());
        
        employee.setUsername(username);
        if (fullName != null) employee.setFullName(fullName);
        if (email != null) employee.setEmail(email);
        
        // If new employee, set some defaults? Could leave position/phone null.
        return employeeRepository.save(employee);
    }
}
