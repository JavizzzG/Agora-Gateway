package com.agora.gateway.domain.model;

import java.util.List;

/**
 * Domain-level list of paths that bypass JWT authentication.
 */
public class PublicRoute {

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/public/auth/authenticate", // Login
            "/public/auth/refresh", // Refresh token
            "/actuator/health",
            "/users/create",
            "/public/auth/google/callback"); // Google OAuth callback


    // Returns true when the route is publicly accessible.
    public static boolean matches(String path){
        return PUBLIC_PREFIXES.stream()
                .anyMatch(path::startsWith);
    }
}
