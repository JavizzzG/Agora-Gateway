package com.agora.gateway.domain.model;

public class UserContext {

    private final String userId;

    public UserContext(String userId){
        this.userId = userId;
    }

    // (Factory method) - create the user context from a token
    public static UserContext from(AuthToken token){
        return new UserContext(token.getUserId());
    }

    public String getUserId() {return userId;};

}
