package com.example.test.printsv.service;

import com.example.test.printsv.entity.ERole;
import com.example.test.printsv.entity.Role;
import com.example.test.printsv.entity.User;
import com.example.test.printsv.repository.RoleRepository;
import com.example.test.printsv.repository.UserRepository;
import com.example.test.printsv.request.LoginRequest;
import com.example.test.printsv.request.RegisterRequest;
import com.example.test.printsv.response.LoginResponse;
import com.example.test.printsv.response.MessageResponse;
import com.example.test.printsv.utils.JwtUtil;
import org.springdoc.webmvc.core.service.RequestService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;


    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, RoleRepository roleRepository, JwtUtil jwtUtil, RequestService requestBuilder) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;

    }

    public LoginResponse authenticate(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        return new LoginResponse(token);
    }

    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return new MessageResponse("Error: Username is already taken!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<String> strRoles = request.getRoles().stream().map(role -> role.toString()).collect(Collectors.toSet());
        Set<Role> roles = request.getRoles();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName(ERole.ROLE_MANAGER)
                    .orElseThrow(() -> new RuntimeException("Error: Role not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                if ("admin".equalsIgnoreCase(role)) {
                    Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                            .orElseThrow(() -> new RuntimeException("Error: Role not found."));
                    roles.add(adminRole);
                } else if ("operator".equalsIgnoreCase(role)) {
                    Role operatorRole = roleRepository.findByName(ERole.ROLE_OPERATOR)
                            .orElseThrow(() -> new RuntimeException("Error: Role not found."));
                    roles.add(operatorRole);
                } else {
                    Role userRole = roleRepository.findByName(ERole.ROLE_MANAGER)
                            .orElseThrow(() -> new RuntimeException("Error: Role not found."));
                    roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return new MessageResponse("User registered successfully!");
    }

    public void registerForInit(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = new HashSet<>();
        for (Role role : request.getRoles()) {
            Role savedRole = roleRepository.save(role);
            roles.add(savedRole);
        }

        user.setRoles(roles);
        userRepository.save(user);
    }
}
