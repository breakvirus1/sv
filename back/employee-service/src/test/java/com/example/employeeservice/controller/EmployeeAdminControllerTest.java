package com.example.employeeservice.controller;

import com.example.employeeservice.config.SecurityConfig;
import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.service.EmployeeAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmployeeAdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class EmployeeAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeAdminService employeeAdminService;

    // ===================== POST /api/v1/admin/employees/generate =====================

    @Nested
    @DisplayName("POST /api/v1/admin/employees/generate")
    class GenerateTestEmployees {

        @Test
        @DisplayName("ADMIN can generate test employees")
        @WithMockUser(roles = "ADMIN")
        void adminCanGenerateTestEmployees() throws Exception {
            Employee employee = new Employee();
            employee.setUsername("user1");
            employee.setFullName("Иванов Иван");
            employee.setPosition("Менеджер");

            when(employeeAdminService.generateTestEmployees(eq(20), isNull())).thenReturn(List.of(employee));

            mockMvc.perform(post("/api/v1/admin/employees/generate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].username").value("user1"))
                    .andExpect(jsonPath("$[0].fullName").value("Иванов Иван"));

            verify(employeeAdminService).generateTestEmployees(eq(20), isNull());
        }

        @Test
        @DisplayName("GOD cannot generate test employees (ADMIN only)")
        @WithMockUser(roles = "GOD")
        void godCannotGenerateTestEmployees() throws Exception {
            mockMvc.perform(post("/api/v1/admin/employees/generate"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MANAGER cannot generate test employees")
        @WithMockUser(roles = "MANAGER")
        void managerCannotGenerateTestEmployees() throws Exception {
            mockMvc.perform(post("/api/v1/admin/employees/generate"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated users get 401")
        @WithAnonymousUser
        void unauthenticatedGets401() throws Exception {
            mockMvc.perform(post("/api/v1/admin/employees/generate"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
