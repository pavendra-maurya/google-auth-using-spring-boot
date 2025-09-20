package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfo {
    private String sub;
    private String email;
    
    @JsonProperty("email_verified")
    private Boolean emailVerified;
    
    private String name;
    
    @JsonProperty("given_name")
    private String givenName;
    
    @JsonProperty("family_name")
    private String familyName;
    
    private String picture;
    private String locale;
    private String aud;
    private Long exp;
    private Long iat;
}	