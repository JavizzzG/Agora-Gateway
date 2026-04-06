package com.agora.gateway.application.usecase;

import com.agora.gateway.domain.model.UserContext;
import com.agora.gateway.domain.ports.in.AuthenticationPort;
import com.agora.gateway.domain.ports.out.TokenValidatorPort;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationUseCase implements AuthenticationPort {

    // Dependencia hacia afuera — a través del puerto, no directo a JJWT
    private final TokenValidatorPort tokenValidator;

    public AuthenticationUseCase(TokenValidatorPort tokenValidator){
        this.tokenValidator = tokenValidator;
    }

    @Override
    public UserContext authenticate(String token) throws AuthenticationException {

        if(token == null || token.isBlank()){
            throw new AuthenticationException("Token was not found", AuthenticationException.Reason.TOKEN_MISSING);
        }

        var authToken = tokenValidator.validate(token);

        return UserContext.from(authToken);

    }

}
