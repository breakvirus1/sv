package com.example.commentservice.controller;

import com.example.commentservice.config.SecurityConfig;
import com.example.commentservice.dto.response.CommentResponse;
import com.example.commentservice.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommentController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private CommentResponse commentResponse() {
        CommentResponse response = new CommentResponse();
        response.setId(1L);
        response.setOrderId(100L);
        response.setEmployeeId(10L);
        response.setEmployeeName("Иван Иванов");
        response.setReaded(false);
        response.setBody("Готово к печати");
        response.setCreatedAt(LocalDateTime.of(2026, 10, 17, 12, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 10, 17, 12, 0));
        response.setImages(List.of());
        response.setReplies(List.of());
        return response;
    }

    // ===================== GET /api/v1/comments =====================

    @Nested
    @DisplayName("GET /api/v1/comments")
    class GetAllComments {

        @Test
        @DisplayName("ADMIN can list comments")
        @WithMockUser(roles = "ADMIN")
        void adminCanListComments() throws Exception {
            Page<CommentResponse> page = new PageImpl<>(
                    List.of(commentResponse()), PageRequest.of(0, 20), 1);
            when(commentService.getAllComments(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].orderId").value(100))
                    .andExpect(jsonPath("$.content[0].body").value("Готово к печати"));
        }

        @Test
        @DisplayName("MANAGER can list comments")
        @WithMockUser(roles = "MANAGER")
        void managerCanListComments() throws Exception {
            Page<CommentResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(commentService.getAllComments(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // ===================== GET /api/v1/comments/order/{orderId} =====================

    @Nested
    @DisplayName("GET /api/v1/comments/order/{orderId}")
    class GetCommentsByOrderId {

        @Test
        @DisplayName("Authenticated user can get comments by order id")
        @WithMockUser(roles = "USER")
        void authenticatedUserCanGetComments() throws Exception {
            when(commentService.getCommentsByOrderId(100L))
                    .thenReturn(List.of(commentResponse()));

            mockMvc.perform(get("/api/v1/comments/order/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].employeeName").value("Иван Иванов"));
        }
    }

    // ===================== Authentication / Authorization =====================

    @Nested
    @DisplayName("Authentication and authorization")
    class AuthChecks {

        @Test
        @DisplayName("Anonymous gets 401 on GET /api/v1/comments")
        @WithAnonymousUser
        void anonymousListGets401() throws Exception {
            mockMvc.perform(get("/api/v1/comments"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous gets 401 on GET /api/v1/comments/order/{orderId}")
        @WithAnonymousUser
        void anonymousByOrderGets401() throws Exception {
            mockMvc.perform(get("/api/v1/comments/order/100"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
