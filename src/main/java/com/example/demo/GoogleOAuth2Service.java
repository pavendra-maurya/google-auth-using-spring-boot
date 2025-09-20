
package com.example.demo;

import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.oauth2.sdk.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleOAuth2Service {
    
    private final RestTemplate restTemplate;
    private final GoogleOAuth2Config config;
    private final RedisTemplate<String, String> redisTemplate;
    
    public GoogleOAuth2Service(RestTemplate restTemplate, 
                             GoogleOAuth2Config config,
                             RedisTemplate<String, String> redisTemplate) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.redisTemplate = redisTemplate;
    }
    
    public String generateAuthorizationUrl(String redirectUri) {
        String state = generateSecureState();
        String finalRedirectUri = redirectUri != null ? redirectUri : config.getDefaultRedirectUri();
        
        // Store state in Redis with expiration
        redisTemplate.opsForValue().set("oauth_state:" + state, "valid", 
                Duration.ofMinutes(10));
        
        return UriComponentsBuilder.fromHttpUrl(config.getAuthorizationUri())
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", finalRedirectUri)
                .queryParam("scope", "openid email profile")
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .build().toUriString();
    }
    
    public GoogleTokenResponse exchangeCodeForTokens(String code, String redirectUri) {
        if (StringUtils.isBlank(code)) {
            throw new InvalidAuthorizationCodeException("Authorization code cannot be empty");
        }
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        
        try {
            ResponseEntity<GoogleTokenResponse> response = restTemplate.exchange(
                    config.getTokenUri(),
                    HttpMethod.POST,
                    request,
                    GoogleTokenResponse.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GoogleOAuth2Exception("Failed to exchange authorization code");
            }
            
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            log.warn("Google OAuth2 client error: {}", e.getResponseBodyAsString());
            throw new InvalidAuthorizationCodeException("Invalid authorization code");
        } catch (RestClientException e) {
            log.error("Google OAuth2 service error", e);
            throw new GoogleOAuth2Exception("OAuth2 service unavailable");
        }
    }
    
    public GoogleUserInfo validateAndExtractUserInfo(String idToken) {
        if (StringUtils.isBlank(idToken)) {
            throw new GoogleOAuth2Exception("ID token is missing");
        }
        
        try {
            // Validate token with Google
            String validationUrl = config.getTokenInfoUri() + "?id_token=" + idToken;
            ResponseEntity<GoogleUserInfo> response = restTemplate.getForEntity(
                    validationUrl, GoogleUserInfo.class);
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GoogleOAuth2Exception("Invalid ID token");
            }
            
            GoogleUserInfo userInfo = response.getBody();
            
            // Validate token audience (client_id)
            if (!config.getClientId().equals(userInfo.getAud())) {
                throw new GoogleOAuth2Exception("Invalid token audience");
            }
            
            // Validate token expiration
            if (userInfo.getExp() != null && userInfo.getExp() < System.currentTimeMillis() / 1000) {
                throw new GoogleOAuth2Exception("Token expired");
            }
            
            return userInfo;
            
        } catch (RestClientException e) {
            log.error("Failed to validate Google ID token", e);
            throw new GoogleOAuth2Exception("Token validation failed");
        }
    }
    
    private String generateSecureState() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().getBytes());
    }
}
