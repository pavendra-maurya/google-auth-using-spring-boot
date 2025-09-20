package com.example.demo;


@RestController
@RequestMapping("/auth/google")
@Slf4j
@Validated
public class GoogleAuthController {
    
    private final GoogleOAuth2Service googleOAuth2Service;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final GoogleOAuth2Config googleOAuth2Config;
    
    public GoogleAuthController(GoogleOAuth2Service googleOAuth2Service,
                              UserService userService,
                              JwtUtil jwtUtil,
                              GoogleOAuth2Config googleOAuth2Config) {
        this.googleOAuth2Service = googleOAuth2Service;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.googleOAuth2Config = googleOAuth2Config;
    }
    
    @PostMapping("/callback")
    @Operation(summary = "Handle Google OAuth2 callback", description = "Process Google OAuth2 authorization code and return JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "429", description = "Too many requests"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @RateLimiter(name = "google-auth")
    public ResponseEntity<AuthResponse> handleGoogleCallback(
            @Valid @RequestBody GoogleCallbackRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Processing Google OAuth callback for IP: {}", 
                    getClientIpAddress(httpRequest));
            
            // Validate and exchange authorization code for tokens
            GoogleTokenResponse tokenResponse = googleOAuth2Service.exchangeCodeForTokens(
                request.getCode(), 
                request.getRedirectUri()
            );
            
            // Validate and extract user info from ID token
            GoogleUserInfo userInfo = googleOAuth2Service.validateAndExtractUserInfo(
                tokenResponse.getIdToken()
            );
            
            // Find or create user
            User user = userService.findOrCreateGoogleUser(userInfo);
            
            // Generate JWT token with proper expiration
            String jwtToken = jwtUtil.generateToken(user.getEmail(), user.getRoles());
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
            
            // Log successful authentication
            log.info("Successful Google authentication for user: {}", 
                    maskEmail(user.getEmail()));
            
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(jwtToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtUtil.getTokenExpirationTime())
                    .tokenType("Bearer")
                    .user(UserResponse.fromUser(user))
                    .build());
                    
        } catch (InvalidAuthorizationCodeException e) {
            log.warn("Invalid authorization code provided from IP: {}", 
                    getClientIpAddress(httpRequest));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.error("Invalid authorization code"));
                    
        } catch (GoogleOAuth2Exception e) {
            log.error("Google OAuth2 error from IP: {}", 
                    getClientIpAddress(httpRequest), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Authentication failed"));
                    
        } catch (UserCreationException e) {
            log.error("Failed to create user from IP: {}", 
                    getClientIpAddress(httpRequest), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Failed to create user account"));
                    
        } catch (Exception e) {
            log.error("Unexpected error during Google authentication from IP: {}", 
                    getClientIpAddress(httpRequest), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Internal server error"));
        }
    }
    
    @GetMapping("/login-url")
    @Operation(summary = "Get Google OAuth2 login URL")
    public ResponseEntity<GoogleLoginUrlResponse> getGoogleLoginUrl(
            @RequestParam(required = false) String redirectUri) {
        
        try {
            String loginUrl = googleOAuth2Service.generateAuthorizationUrl(redirectUri);
            return ResponseEntity.ok(new GoogleLoginUrlResponse(loginUrl));
        } catch (Exception e) {
            log.error("Failed to generate Google login URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}