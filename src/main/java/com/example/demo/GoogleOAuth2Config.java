package com.example.demo;


@ConfigurationProperties(prefix = "app.oauth2.google")
@Configuration
@Data
@Validated
public class GoogleOAuth2Config {
    @NotBlank
    private String clientId;
    
    @NotBlank
    private String clientSecret;
    
    @NotBlank
    private String authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth";
    
    @NotBlank
    private String tokenUri = "https://oauth2.googleapis.com/token";
    
    @NotBlank
    private String tokenInfoUri = "https://oauth2.googleapis.com/tokeninfo";
    
    @NotBlank
    private String defaultRedirectUri;
}
