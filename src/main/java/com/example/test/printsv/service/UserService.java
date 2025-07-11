package com.example.test.printsv.service;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(User user) {
        log.info("Создаем юзера: {}", user.getName());
        return userRepository.save(user);
    }

    @Transactional
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь с id " + id + " не найден"));
    }

    @Transactional
    public User getUserByName(String name) {
        return userRepository.findByName(name);
    }

    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь с id " + id + " не найден"));
        existingUser.setName(updatedUser.getName());
        existingUser.setRole(updatedUser.getRole());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        return userRepository.save(existingUser);
    }

    public User saveUser(User user) {
        user.setId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        log.info("Сохранен юзер: {}", user.getName());
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Пользователь с id " + id + " не найден");
        }
        userRepository.deleteById(id);
    }

    public boolean checkUserAuthorization(Long userId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("Пользователь с id " + userId + " не найден"));
        return user.getName().equals(username);
    }
}