package com.example.test.printsv.controller;

import java.util.*;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.repository.UserRepository;
import com.example.test.printsv.request.UserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.test.printsv.response.UserResponse;
import com.example.test.printsv.service.UserService;

import lombok.AllArgsConstructor;


@Tag(name="Юзеры", description = "CRUD действия над юзерами")
@RestController
@AllArgsConstructor
@RequestMapping("/api/user")
//@PreAuthorize("hasRole('ADMIN')")

public class UserController {

    private UserService userService;

    private final UserRepository userRepository;

    @Operation(summary = "Получить всех пользователей", description = "Возвращает список всех пользователей (только для администратора)")
    @PreAuthorize("ROLE_ADMIN")
    @GetMapping
    public List<UserResponse> getAllUsers() {
        
        return userService.getAllUsers();
    }

    @Operation(summary = "Получить пользователя по id", description = "Возвращает пользователя по идентификатору (только для администратора)")
    @Secured("ROLE_ADMIN")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@Parameter(description = "Id пользователя") @PathVariable("id") Long id,
                                     Locale locale) {
        try{
            User userRequest = userService.getUserById(id);
            return ResponseEntity.ok(userRequest);
        }
        catch (RuntimeException ex){
            return ResponseEntity.status(404).body(locale);
        }
    }
//
//    @Operation(summary = "Создать пользователя", description = "Создаёт нового пользователя (только для администратора)")
//    @Secured("ROLE_ADMIN")
//    @PostMapping
//    public ResponseEntity<?> create(@RequestBody User user) {
//        return ResponseEntity.status(201).body(userService.(user));
//    }

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