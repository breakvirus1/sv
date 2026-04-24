package com.example.userservice.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.common.entity.User;
import com.example.userservice.response.UserResponse;
import com.example.userservice.service.UserService;
import com.example.userservice.service.ZakazService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ZakazService zakazService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Long id,
                                     Locale locale) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body(locale);
        }
    }

    @GetMapping("/me-id")
    public ResponseEntity<?> getMyId() {
        return ResponseEntity.ok(userService.getCurrentUserId());
    }

    @GetMapping("/zakaz/all")
    public ResponseEntity<?> getZakazList(@RequestParam("id") Long id) {
        // Placeholder - will be implemented via REST call later
        throw new UnsupportedOperationException("Zakaz service not available in user-service yet");
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable("id") Long id,
                                         @RequestBody User updatedUser) {
        return ResponseEntity.ok(userService.updateUser(id, updatedUser));
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable("id") Long id, Locale locale) {
        userService.deleteUser(id);
        return ResponseEntity.ok(201);
    }
}
