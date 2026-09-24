package com.lexhive.libraryapp.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This is list of predefined users that can be used for authentication and authorization in this Application.
 * For real world application, this should be stored in a database or a secure storage.
 */
@ConfigurationProperties(prefix = "app.security")
public record StaticUsers(List<UserAccount> users) {

    public record UserAccount(String username, String password, String role) {
    }
}
