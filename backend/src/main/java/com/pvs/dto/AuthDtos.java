package com.pvs.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {}

    public record LoginResponse(
            String token,
            String username,
            String role) {}

    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String role) {}

    public record UserResponse(
            Long id,
            String username,
            String role,
            boolean enabled) {}
}
