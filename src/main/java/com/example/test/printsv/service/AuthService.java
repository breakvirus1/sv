package com.example.test.printsv.service;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.request.RegisterRequest;
import com.example.test.printsv.request.SignUpRequest;
import com.example.test.printsv.response.SignUpResponse;
import com.example.test.printsv.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor // в конструктор войдут только обязательные поля
public class AuthService {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenUtils;
    private final AuthenticationManager authenticationManager; //стандартный Spring Security компонент для аутентификации

    public ResponseEntity<?> createAuthToken(@RequestBody SignUpRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(), "Неправильный логин или пароль"),
                    HttpStatus.UNAUTHORIZED);
        }
        UserDetails userDetails = userService.loadUserByUsername(authRequest.getUsername());
        String token = jwtTokenUtils.generateToken(userDetails);
        return ResponseEntity.ok(new SignUpResponse(token));
    }

    public ResponseEntity<?> createNewUser(@RequestBody RegisterRequest registrationUserDto) {
        if (!registrationUserDto.getPassword().equals(registrationUserDto.getPassword())) {
            return new ResponseEntity<>(new AppError(HttpStatus.BAD_REQUEST.value(), "Пароли не совпадают"),
                    HttpStatus.BAD_REQUEST);
        }
        if (userService.findByUsername(registrationUserDto.getUsername()).isPresent()) {
            return new ResponseEntity<>(
                    new AppError(HttpStatus.CONFLICT.value(), "Пользователь с указанным именем уже существует"),
                    HttpStatus.CONFLICT);
        }
        User user = userService.registerUser(registrationUserDto);

        return ResponseEntity.ok(201);
    }
}
