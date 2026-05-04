package com.example.employeeservice.service;

import com.example.employeeservice.dto.EmployeeCreateRequest;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.employeeservice.dto.EmployeeUpdateRequest;
import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для управления сотрудниками.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public Page<EmployeeResponse> getAllEmployees(Specification<Employee> spec, Pageable pageable) {
        return employeeRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
        return mapToResponse(employee);
    }

    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        if (employeeRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Сотрудник с таким логином уже существует");
        }
        Employee employee = new Employee();
        employee.setFullName(request.getFullName());
        employee.setUsername(request.getUsername());
        employee.setPosition(request.getPosition());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = getEmployeeEntity(id);
        employee.setFullName(request.getFullName());
        employee.setUsername(request.getUsername());
        employee.setPosition(request.getPosition());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    public void deleteEmployee(Long id) {
        Employee employee = getEmployeeEntity(id);
        employeeRepository.delete(employee);
    }

    public EmployeeResponse syncOrCreateFromKeycloak(Jwt jwt) {
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

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    private Employee getEmployeeEntity(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
    }

    @Transactional(readOnly = true)
    private EmployeeResponse mapToResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getPosition(),
                employee.getPhone(),
                employee.getEmail(),
                employee.getUsername()
        );
    }
}
