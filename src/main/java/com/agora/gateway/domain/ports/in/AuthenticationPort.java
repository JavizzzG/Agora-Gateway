package com.agora.gateway.domain.ports.in;

import com.agora.gateway.domain.model.UserContext;

public interface AuthenticationPort {

    UserContext authenticate(String token) throws AuthenticationException;

    class AuthenticationException extends Exception {

        public enum Reason {
            TOKEN_EXPIRED,
            TOKEN_INVALID,
            TOKEN_MALFORMED,
            TOKEN_MISSING
        }

        private final Reason reason;

        public AuthenticationException(String message, Reason reason){
            super(message);
            this.reason = reason;
        }

        public Reason getReason() {return reason; }
    }

}
