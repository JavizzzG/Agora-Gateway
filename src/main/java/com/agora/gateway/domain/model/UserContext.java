package com.agora.gateway.domain.model;

/**
 * Minimal authenticated user context propagated to downstream services.
 */
public class UserContext {

    private final String userId;

    public UserContext(String userId){
        this.userId = userId;
    }

    // Factory method that extracts user context from validated token data.
    public static UserContext from(AuthToken token){
        return new UserContext(token.getUserId());
    }

    public String getUserId() {return userId;};

}
