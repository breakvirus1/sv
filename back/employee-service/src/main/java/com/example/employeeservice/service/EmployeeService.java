package com.example.employeeservice.service;

import com.example.employeeservice.dto.EmployeeCreateRequest;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.employeeservice.dto.EmployeeUpdateRequest;
import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.entity.ERole;
import com.example.employeeservice.entity.Role;
import com.example.employeeservice.mapper.EmployeeMapper;
import com.example.employeeservice.repository.EmployeeRepository;
import com.example.employeeservice.repository.RoleRepository;
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
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final RoleRepository roleRepository;

    @Value("${keycloak.admin.server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin.realm:print-sv}")
    private String keycloakRealm;

    @Value("${keycloak.admin.client-id:order-service}")
    private String keycloakClientId;

    @Value("${keycloak.admin.client-secret:order-service-secret}")
    private String keycloakClientSecret;

    public Page<EmployeeResponse> getAllEmployees(Specification<Employee> spec, Pageable pageable) {
        Page<EmployeeResponse> employees = employeeRepository.findAll(spec, pageable)
                .map(employeeMapper::toDto);

        // Populate roles from DB
        try {
            employees.getContent().forEach(emp -> {
                Employee entity = employeeRepository.findById(emp.getId()).orElse(null);
                if (entity != null && entity.getRoles() != null) {
                    List<String> roleNames = entity.getRoles().stream()
                            .map(r -> r.getRoleName())
                            .filter(java.util.Objects::nonNull)
                            .map(ERole::name)
                            .toList();
                    emp.setRoles(roleNames);
                }
            });
        } catch (Exception e) {
            // Roles table may not exist yet, return without roles
        }

        return employees;
    }

    /**
     * Fetch Keycloak realm roles for given usernames.
     * Returns map of username -> list of role names.
     */
    private Map<String, List<String>> fetchKeycloakRolesForUsernames(List<String> usernames) {
        Map<String, List<String>> result = new HashMap<>();
        if (usernames == null || usernames.isEmpty()) return result;

        try {
            RestTemplate restTemplate = new RestTemplate();
            String adminToken = obtainAdminToken(restTemplate);

            for (String username : usernames) {
                try {
                    List<String> roles = fetchUserRoles(restTemplate, adminToken, username);
                    result.put(username, roles);
                } catch (Exception e) {
                    result.put(username, List.of());
                }
            }
        } catch (Exception e) {
            // Return empty map on failure
        }
        return result;
    }

    private List<String> fetchUserRoles(RestTemplate restTemplate, String adminToken, String username) {
        String usersUrl = keycloakServerUrl + "/admin/realms/" + keycloakRealm + "/users?username=" + username + "&exact=true";
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(adminToken);
        var userRequest = new org.springframework.http.HttpEntity<>(headers);
        var response = restTemplate.exchange(usersUrl, org.springframework.http.HttpMethod.GET, userRequest, Object[].class);

        if (response.getBody() == null || response.getBody().length == 0) {
            return List.of();
        }

        Map<String, Object> kcUser = (Map<String, Object>) response.getBody()[0];
        String userId = (String) kcUser.get("id");
        if (userId == null) return List.of();

        // Fetch role mappings for this user
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId + "/role-mappings/realm";
        var rolesRequest = new org.springframework.http.HttpEntity<>(headers);
        var rolesResponse = restTemplate.exchange(rolesUrl, org.springframework.http.HttpMethod.GET, rolesRequest, Object[].class);

        if (rolesResponse.getBody() == null) return List.of();

        List<String> roles = new java.util.ArrayList<>();
        for (Object item : rolesResponse.getBody()) {
            Map<String, Object> role = (Map<String, Object>) item;
            String roleName = (String) role.get("name");
            if (roleName != null) {
                roles.add(roleName);
            }
        }
        return roles;
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
        EmployeeResponse dto = employeeMapper.toDto(employee);
        // Fetch roles from Keycloak
        if (employee.getUsername() != null) {
            Map<String, List<String>> rolesMap = fetchKeycloakRolesForUsernames(List.of(employee.getUsername()));
            dto.setRoles(rolesMap.getOrDefault(employee.getUsername(), List.of()));
        }
        return dto;
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

        // Sync roles from Keycloak
        List<String> kcRoles = fetchKeycloakRolesForUsernames(List.of(username)).getOrDefault(username, List.of());
        java.util.Set<Role> roles = new java.util.HashSet<>();
        Long firstRoleId = null;
        for (String roleName : kcRoles) {
            ERole eRole = mapKeycloakRoleName(roleName);
            if (eRole == null) continue;
            try {
                Role role = roleRepository.findByName(eRole)
                        .orElseGet(() -> roleRepository.save(new Role(eRole)));
                roles.add(role);
                if (firstRoleId == null) {
                    firstRoleId = role.getId();
                }
            } catch (Exception e) {
                // Skip problematic roles
            }
        }
        employee.setRoles(roles);
        employee.setRoleId(firstRoleId);

        Employee saved = employeeRepository.save(employee);
        EmployeeResponse dto = employeeMapper.toDto(saved);
        dto.setRoles(kcRoles);
        return dto;
    }

    @Transactional
    public String syncAllFromKeycloak() {
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
        int updated = 0;
        for (var kcUser : kcUsers) {
            String username = (String) kcUser.get("username");
            if (username == null) continue;

            List<String> kcRoles = fetchUserRoles(restTemplate, adminToken, username);
            java.util.Set<Role> roles = new java.util.HashSet<>();
            Long firstRoleId = null;
            for (String roleName : kcRoles) {
                ERole eRole = mapKeycloakRoleName(roleName);
                if (eRole == null) continue;
                try {
                    Role role = roleRepository.findByName(eRole)
                            .orElseGet(() -> roleRepository.save(new Role(eRole)));
                    roles.add(role);
                    if (firstRoleId == null) {
                        firstRoleId = role.getId();
                    }
                } catch (Exception e) {
                    // Skip problematic roles
                }
            }

            if (existingUsernames.contains(username)) {
                Employee existing = employeeRepository.findByUsername(username).orElse(null);
                if (existing != null) {
                    existing.setRoles(roles);
                    existing.setRoleId(firstRoleId);
                    if (kcUser.get("firstName") != null) {
                        String firstName = (String) kcUser.get("firstName");
                        String lastName = (String) kcUser.get("lastName");
                        existing.setFullName(firstName + " " + (lastName != null ? lastName : ""));
                    }
                    if (kcUser.get("email") != null) {
                        existing.setEmail((String) kcUser.get("email"));
                    }
                    employeeRepository.save(existing);
                    updated++;
                }
                continue;
            }

            Employee employee = new Employee();
            employee.setUsername(username);
            String firstName = (String) kcUser.get("firstName");
            String lastName = (String) kcUser.get("lastName");
            employee.setFullName(firstName != null
                    ? (firstName + " " + (lastName != null ? lastName : "")).trim()
                    : username);
            employee.setEmail((String) kcUser.get("email"));
            employee.setRoles(roles);
            employee.setRoleId(firstRoleId);

            employeeRepository.save(employee);
            existingUsernames.add(username);
            created++;
        }

        return "created=" + created + ", updated=" + updated;
    }

    private ERole mapKeycloakRoleName(String kcRoleName) {
        if (kcRoleName == null) return null;
        switch (kcRoleName) {
            case "ADMIN":       return ERole.ROLE_ADMIN;
            case "MANAGER":     return ERole.ROLE_MANAGER;
            case "PRODUCTION":  return ERole.ROLE_PRODUCTION;
            case "ACCOUNTANT":  return ERole.ROLE_ACCOUNTANT;
            case "USER":        return ERole.ROLE_USER;
            default:
                try {
                    return ERole.valueOf(kcRoleName);
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }
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

    /**
     * Get Keycloak roles for a specific user by username.
     */
    public List<String> getKeycloakRolesForUser(String username) {
        if (username == null || username.isEmpty()) return List.of();
        Map<String, List<String>> rolesMap = fetchKeycloakRolesForUsernames(List.of(username));
        return rolesMap.getOrDefault(username, List.of());
    }
}
