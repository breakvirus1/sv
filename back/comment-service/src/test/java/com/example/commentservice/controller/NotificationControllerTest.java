package com.example.commentservice.controller;

import com.example.commentservice.config.SecurityConfig;
import com.example.commentservice.dto.response.NotificationResponse;
import com.example.commentservice.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private NotificationResponse notificationResponse() {
        NotificationResponse response = new NotificationResponse();
        response.setId(1L);
        response.setUserId(10L);
        response.setMessage("Комментарий к заказу");
        response.setType("COMMENT");
        response.setReferenceId(100L);
        response.setReferenceType("ORDER");
        response.setReferenceSubId(null);
        response.setReaded(false);
        response.setCreatedAt(LocalDateTime.of(2026, 10, 17, 12, 0));
        return response;
    }

    // ===================== GET /api/v1/notifications =====================

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class GetNotifications {

        @Test
        @DisplayName("Returns 200 with current user notifications")
        @WithMockUser(roles = "USER")
        void returnsNotifications() throws Exception {
            when(notificationService.getNotificationsForCurrentUser())
                    .thenReturn(List.of(notificationResponse()));

            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].userId").value(10))
                    .andExpect(jsonPath("$[0].message").value("Комментарий к заказу"))
                    .andExpect(jsonPath("$[0].type").value("COMMENT"))
                    .andExpect(jsonPath("$[0].readed").value(false));
        }
    }

    // ===================== GET /api/v1/notifications/unread-count =====================

    @Nested
    @DisplayName("GET /api/v1/notifications/unread-count")
    class GetUnreadCount {

        @Test
        @DisplayName("Returns 200 with unread count")
        @WithMockUser(roles = "USER")
        void returnsUnreadCount() throws Exception {
            when(notificationService.getUnreadCountForCurrentUser()).thenReturn(5L);

            mockMvc.perform(get("/api/v1/notifications/unread-count"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        }
    }

    // ===================== PATCH /api/v1/notifications/{id}/read =====================

    @Nested
    @DisplayName("PATCH /api/v1/notifications/{id}/read")
    class MarkAsRead {

        @Test
        @DisplayName("Returns 200 with updated notification")
        @WithMockUser(roles = "USER")
        void returns200WithUpdatedNotification() throws Exception {
            NotificationResponse read = notificationResponse();
            read.setReaded(true);
            when(notificationService.markAsRead(1L)).thenReturn(read);

            mockMvc.perform(patch("/api/v1/notifications/1/read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.readed").value(true));
        }
    }

    // ===================== PATCH /api/v1/notifications/read-all =====================

    @Nested
    @DisplayName("PATCH /api/v1/notifications/read-all")
    class MarkAllAsRead {

        @Test
        @DisplayName("Returns 200 when all notifications are marked as read")
        @WithMockUser(roles = "USER")
        void returns200() throws Exception {
            doNothing().when(notificationService).markAllAsRead();

            mockMvc.perform(patch("/api/v1/notifications/read-all"))
                    .andExpect(status().isOk());

            verify(notificationService).markAllAsRead();
        }
    }

    // ===================== DELETE /api/v1/notifications/{id} =====================

    @Nested
    @DisplayName("DELETE /api/v1/notifications/{id}")
    class DeleteNotification {

        @Test
        @DisplayName("Returns 204 when notification is deleted")
        @WithMockUser(roles = "USER")
        void returns204() throws Exception {
            doNothing().when(notificationService).deleteNotification(anyLong());

            mockMvc.perform(delete("/api/v1/notifications/1"))
                    .andExpect(status().isNoContent());

            verify(notificationService).deleteNotification(1L);
        }
    }

    // ===================== Authentication / Authorization =====================

    @Nested
    @DisplayName("Authentication and authorization")
    class AuthChecks {

        @Test
        @DisplayName("Anonymous gets 401 on GET /api/v1/notifications")
        @WithAnonymousUser
        void anonymousGetListGets401() throws Exception {
            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous gets 401 on GET /api/v1/notifications/unread-count")
        @WithAnonymousUser
        void anonymousGetUnreadCountGets401() throws Exception {
            mockMvc.perform(get("/api/v1/notifications/unread-count"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous gets 401 on PATCH /api/v1/notifications/{id}/read")
        @WithAnonymousUser
        void anonymousMarkAsReadGets401() throws Exception {
            mockMvc.perform(patch("/api/v1/notifications/1/read"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous gets 401 on PATCH /api/v1/notifications/read-all")
        @WithAnonymousUser
        void anonymousMarkAllAsReadGets401() throws Exception {
            mockMvc.perform(patch("/api/v1/notifications/read-all"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous gets 401 on DELETE /api/v1/notifications/{id}")
        @WithAnonymousUser
        void anonymousDeleteGets401() throws Exception {
            mockMvc.perform(delete("/api/v1/notifications/1"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
