
package com.example.demo;

import java.util.Arrays;

import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public User findOrCreateGoogleUser(GoogleUserInfo googleUserInfo) {
        String email = googleUserInfo.getEmail();
        
        if (StringUtils.isBlank(email)) {
            throw new UserCreationException("Email is required");
        }
        
        return userRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(googleUserInfo));
    }
    
    private User createGoogleUser(GoogleUserInfo googleUserInfo) {
        try {
            User user = User.builder()
                    .emailgoogleUserInfo.getEmail())
                    .name(googleUserInfo.getName())
                    .userName(googleUserInfo.getEmail())
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .roles(Arrays.asList("USER"))
                    .provider("GOOGLE")
                    .providerId(googleUserInfo.getSub())
                    .emailVerified(googleUserInfo.getEmailVerified())
                    .profilePicture(googleUserInfo.getPicture())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                    
            return userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to create user for email: {}", googleUserInfo.getEmail(), e);
            throw new UserCreationException("Failed to create user account");
        }
    }
}