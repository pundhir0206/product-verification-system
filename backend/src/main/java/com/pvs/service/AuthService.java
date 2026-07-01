package com.pvs.service;

import com.pvs.dto.AuthDtos.*;
import com.pvs.entity.Role;
import com.pvs.entity.User;
import com.pvs.repository.UserRepository;
import com.pvs.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, user.getUsername(), user.getRole().name());
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        Role role;
        try {
            role = Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role must be ADMIN or OPERATOR");
        }

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .enabled(true)
                .build();
        user = userRepository.save(user);

        return new UserResponse(user.getId(), user.getUsername(), user.getRole().name(), user.isEnabled());
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getRole().name(), u.isEnabled()))
                .toList();
    }
}
