package com.agora.gateway.domain.model;

import java.util.List;

public class PublicRoute {

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/public/auth/authenticate", //Login
            "/public/auth/refresh", //Refresh
            "/actuator/health",
            "/users/create");


    // Respond true if the path not required authentication
    public static Boolean matches(String path){
        return PUBLIC_PREFIXES.stream()
                .anyMatch(path::startsWith);
    }
}
