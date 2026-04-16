package com.agora.gateway.domain.model;

import java.time.Instant;

public class AuthToken {
    private final String userId;
    private final Instant expiresAt;

    public AuthToken(String userId, Instant expiresAt){
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public String getUserId() {return userId;}
    public Instant getExpiresAt() {return expiresAt;}

    public boolean isExpired(){
        return Instant.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "AuthToken{userId='" + userId + "'";
    }
}
