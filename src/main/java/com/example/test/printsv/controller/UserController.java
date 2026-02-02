package com.example.test.printsv.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.repository.UserRepository;
import com.example.test.printsv.response.UserResponse;
import com.example.test.printsv.response.ZakazResponse;
import com.example.test.printsv.service.UserService;
import com.example.test.printsv.service.ZakazService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@Tag(name="Юзеры", description = "CRUD действия над юзерами")
@RestController
@AllArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private UserService userService;
    private ZakazService zakazService;

    private final UserRepository userRepository;

    @Operation(summary = "Получить всех пользователей", description = "Возвращает список всех пользователей (только для администратора)")
    @PreAuthorize("ROLE_ADMIN")
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "Получить пользователя по id", description = "Возвращает пользователя по идентификатору (только для администратора)")
    @PreAuthorize("ROLE_ADMIN")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@Parameter(description = "Id пользователя") @PathVariable("id") Long id,
                                    Locale locale) {
        try {
            User userRequest = userService.getUserById(id);
            return ResponseEntity.ok(userRequest);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body(locale);
        }
    }

    @Operation(summary = "Получить id аутентифицированного пользователя ", description = "Возвращает пользователя")
    @GetMapping("/me-id")
    public ResponseEntity<?> getMyId() {
        return ResponseEntity.ok(userService.getCurrentUserId());
    }

    @Operation(summary = "Получить список заказов пользователя", description = "Возвращает список заказов пользователя")
    @GetMapping("/zakaz/all")
    public ResponseEntity<?> getZakazList(@Parameter(description = "Id пользователя") @RequestParam("id") Long id) {
        List<ZakazResponse> zakazList = zakazService.getAllZakazByUserId(id);
        return ResponseEntity.ok(zakazList);
    }

    @Operation(summary = "Обновить пользователя", description = "Обновляет данные пользователя (только для администратора)")
    @Secured("ROLE_ADMIN")
    @PutMapping("/{id}")
    public ResponseEntity<String> update(@Parameter(description = "Id пользователя") @PathVariable("id") Long id,
                                        @RequestBody User updatedUser) {
        return ResponseEntity.ok(userService.updateUser(id, updatedUser));
    }

    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя по id (только для администратора)")
    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Id пользователя") @PathVariable("id") Long id, Locale locale) {
        userService.deleteUser(id);
        return ResponseEntity.ok(201);
    }
}