package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
    private UserResponse user;
    private String error;
    private String message;
    
    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .error("authentication_failed")
                .message(message)
                .build();
    }
}