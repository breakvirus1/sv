package com.example.test.printsv.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.mapper.UserMapper;
import com.example.test.printsv.repository.UserRepository;
import com.example.test.printsv.response.UserResponse;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service

public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    
    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }


    @PostConstruct
    public void init() {

    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User getUserById(Long id) {
        log.info("запрос пользователя с id: " + id);
        return userRepository.findById(id)
                .orElse(new User());
    }

    @Transactional
    public Optional<User> getUserByName(String name) {
        return userRepository.findByUsername(name);
    }

    @PreAuthorize("ROLE_ADMIN")
    public List<UserResponse> getAllUsers() {
        log.info("запрос всех пользователей");
        if (userMapper == null) {
            throw new IllegalStateException("UserMapper is not initialized");
        }
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
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

    public String getCurrentUserName(){
        String currentUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        
        return currentUserName;
    }
    public Long getCurrentUserId(){
        Optional<User> user = userRepository.findByUsername(getCurrentUserName());
        Long currentUserId = user.get().getId();

        
        return currentUserId;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public String updateUser(Long id, User updatedUser) {
        User existing = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        existing.setUsername(updatedUser.getUsername());
        existing.setPassword(existing.getPassword());
        existing.setRoles(updatedUser.getRoles());
        userRepository.save(existing);
        return existing.getUsername() + " is updated";
    }

    @PreDestroy
    public void destroy() {

    }
}
