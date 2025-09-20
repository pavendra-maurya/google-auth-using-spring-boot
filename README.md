Google Auth using Spring Boot

A sample Spring Boot application demonstrating how to integrate Google OAuth2 login / authentication in a Spring Boot app.

Table of Contents

Features

Prerequisites

Setup & Installation

Configuration

How It Works

Running the Application

Endpoints

Troubleshooting / Common Issues

Contributing

License

Features

Google OAuth2 login via Spring Security

Retrieve basic user info (name, email, etc.)

Simple demo example to showcase OAuth with Google

Minimal dependencies

Prerequisites

Java 21

Maven

Google Cloud account (to obtain OAuth credentials)

An IDE (e.g. IntelliJ, Eclipse) or command-line tools

Setup & Installation

Clone the repository

git clone https://github.com/pavendra-maurya/google-auth-using-spring-boot.git
cd google-auth-using-spring-boot


Import it into your IDE as a Maven project (or use mvn clean install from the command line).

Create OAuth credentials in Google Cloud Console (see next section).

Add the credentials (client id / secret) into application properties / configuration.

Configuration

You need to register your app in the Google Cloud Console to get:

Client ID

Client Secret

Authorized Redirect URI (e.g. http://localhost:8080/login/oauth2/code/google)

Then, configure your Spring Boot application (in application.properties or application.yml). Example:

spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
            scope:
              - openid
              - email
              - profile
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
            user-name-attribute: sub


Adjust as necessary for your domain, port, etc.

How It Works

User attempts to access a secured endpoint.

Spring Security redirects to Google’s OAuth2 consent/authorization screen.

User authorizes access → Google returns an authorization code.

The app exchanges the code for an access token.

With the access token, the app requests the user’s profile data from Google.

Spring injects an OAuth2User (or via AuthenticationPrincipal) with user attributes such as name, email, etc.

The application can now use that for further logic (e.g. saving user record, granting roles, etc.).
