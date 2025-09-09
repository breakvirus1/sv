package com.example.test.printsv.service;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.enums.Role;
import com.example.test.printsv.mapper.UserMapper;
import com.example.test.printsv.repository.UserRepository;
import com.example.test.printsv.request.RegisterRequest;
import com.example.test.printsv.response.UserResponse;


import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service

@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;


    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }


    public User registerUser(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(Role.USER));

        return userRepository.save(user);
    }


    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User getUserById(Long id) {
        log.info("запрос пользователя с id: "+id);

        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь с id " + id + " не найден"));
    }

    @Transactional
    public Optional<User> getUserByName(String name) {
        return userRepository.findByUsername(name);
    }

    public ResponseEntity<UserResponse> getUserInfo(Authentication authentication) {
        String currentUserName = authentication.getName();
        log.info("Запрос информации о пользователе: {}", currentUserName);
        Optional<User> user = userRepository.findByUsername(currentUserName);
        if (user.isPresent()) {
            return ResponseEntity.ok(userMapper.toUserResponse(user.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    public List<UserResponse> getAllUsers() {
        log.info("запрос всех пользователей");

        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());


    }


    public User saveUser(User user) {
        user.setId(null); // Ensure new user
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        log.info("Сохранен пользователь: {}", user.getUsername());
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Пользователь с id " + id + " не найден");
        }
        log.info("Удален пользователь с id: {}", id);
        userRepository.deleteById(id);
    }

    public boolean checkUserAuthorization(Long userId) {
        String currentUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> user = userRepository.findByUsername(currentUserName);
        return user.isPresent() && user.get().getId().equals(userId);
    }


    @Override
    @Transactional //все обращения к БД происходят в одной транзакции
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        System.out.println(user.getUsername());
        System.out.println("ROLES:");
        user.getRoles().forEach(System.out::println);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getRoles().stream()
                        .map(role -> {
                            System.out.println("-> Granted: " + role.name());
                            return new SimpleGrantedAuthority(role.name());
                        })
                        .collect(Collectors.toSet()));

    }

    public User updateUser(Long id, User updatedUser) {
        User existing = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        existing.setUsername(updatedUser.getUsername());
        existing.setPassword(existing.getPassword());
        existing.setRoles(updatedUser.getRoles());

        return saveUser(existing);
    }


    // public boolean hasRole(String userName, String role) {
    //     Optional<User> user = userRepository.findByUsername(userName);
    //     return user.isPresent() && user.get().getRole().equals(role.toString());
    // }


}
