package com.example.employeeservice.service;

import com.example.employeeservice.dto.EmployeeCreateRequest;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.employeeservice.dto.EmployeeUpdateRequest;
import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.mapper.EmployeeMapper;
import com.example.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    @Value("${keycloak.admin.server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin.realm:print-sv}")
    private String keycloakRealm;

    @Value("${keycloak.admin.client-id:order-service}")
    private String keycloakClientId;

    @Value("${keycloak.admin.client-secret:order-service-secret}")
    private String keycloakClientSecret;

    public Page<EmployeeResponse> getAllEmployees(Specification<Employee> spec, Pageable pageable) {
        return employeeRepository.findAll(spec, pageable)
                .map(employeeMapper::toDto);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
        return employeeMapper.toDto(employee);
    }

    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        if (employeeRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Сотрудник с таким логином уже существует");
        }
        Employee employee = employeeMapper.toEntity(request);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toDto(saved);
    }

    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = getEmployeeEntity(id);
        employeeMapper.updateEntityFromRequest(request, employee);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toDto(saved);
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
        return employeeMapper.toDto(saved);
    }

    @Transactional
    public int syncAllFromKeycloak() {
        RestTemplate restTemplate = new RestTemplate();

        String adminToken = obtainAdminToken(restTemplate);

        String usersUrl = keycloakServerUrl + "/admin/realms/" + keycloakRealm + "/users?max=1000";
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(adminToken);
        var request = new org.springframework.http.HttpEntity<>(headers);
        var response = restTemplate.exchange(usersUrl, org.springframework.http.HttpMethod.GET, request, Object[].class);
        java.util.List<Map<String, Object>> kcUsers = new java.util.ArrayList<>();
        if (response.getBody() != null) {
            for (Object item : response.getBody()) {
                kcUsers.add((Map<String, Object>) item);
            }
        }

        Set<String> existingUsernames = employeeRepository.findAllUsernames();

        int created = 0;
        for (var kcUser : kcUsers) {
            String username = (String) kcUser.get("username");
            if (username == null) continue;
            if (existingUsernames.contains(username)) continue;

            Employee employee = new Employee();
            employee.setUsername(username);
            String firstName = (String) kcUser.get("firstName");
            String lastName = (String) kcUser.get("lastName");
            employee.setFullName(firstName != null
                    ? (firstName + " " + (lastName != null ? lastName : "")).trim()
                    : username);
            employee.setEmail((String) kcUser.get("email"));
            employeeRepository.save(employee);
            existingUsernames.add(username);
            created++;
        }

        return created;
    }

    private String obtainAdminToken(RestTemplate restTemplate) {
        org.springframework.http.HttpHeaders tokenHeaders = new org.springframework.http.HttpHeaders();
        tokenHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        // Try admin-cli password grant first
        try {
            String tokenUrl = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";
            var tokenRequest = new org.springframework.http.HttpEntity<>(
                    "grant_type=password&client_id=admin-cli&username=admin&password=admin",
                    tokenHeaders);
            var tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, Map.class);
            if (tokenResponse.getBody() != null && tokenResponse.getBody().get("access_token") != null) {
                return (String) tokenResponse.getBody().get("access_token");
            }
        } catch (Exception ignored) {
        }
        // Fallback: client credentials on master realm
        String tokenUrl = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";
        var tokenRequest = new org.springframework.http.HttpEntity<>(
                "grant_type=client_credentials&client_id=" + keycloakClientId + "&client_secret=" + keycloakClientSecret,
                tokenHeaders);
        var tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, Map.class);
        return (String) tokenResponse.getBody().get("access_token");
    }

    @Transactional(readOnly = true)
    private Employee getEmployeeEntity(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
    }
}
