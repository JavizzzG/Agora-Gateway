package com.agora.gateway.domain.ports.out;

import com.agora.gateway.domain.model.AuthToken;
import com.agora.gateway.domain.ports.in.AuthenticationPort.AuthenticationException;


public interface TokenValidatorPort {

    AuthToken validate(String tokenData) throws AuthenticationException;

}
