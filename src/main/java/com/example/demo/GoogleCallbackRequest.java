package com.example.demo;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCallbackRequest {
    @NotBlank(message = "Authorization code is required")
    @Size(max = 1000, message = "Authorization code too long")
    private String code;
    
    @NotBlank(message = "Redirect URI is required")
    @Pattern(regexp = "^https://.*", message = "Redirect URI must use HTTPS")
    @Size(max = 500, message = "Redirect URI too long")
    private String redirectUri;
    
    @Size(max = 100, message = "State parameter too long")
    private String state;
