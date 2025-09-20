package com.example.demo;

public class InvalidAuthorizationCodeException extends GoogleOAuth2Exception {
    public InvalidAuthorizationCodeException(String message) {
        super(message);
    }
}