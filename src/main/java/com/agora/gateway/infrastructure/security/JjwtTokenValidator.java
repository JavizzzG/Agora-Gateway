package com.agora.gateway.infrastructure.security;

import com.agora.gateway.domain.model.AuthToken;
import com.agora.gateway.domain.ports.in.AuthenticationPort.AuthenticationException;
import com.agora.gateway.domain.ports.in.AuthenticationPort.AuthenticationException.Reason;
import com.agora.gateway.domain.ports.out.TokenValidatorPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;

@Component("jjwtTokenValidator")
public class JjwtTokenValidator implements TokenValidatorPort {

    private final EdDsaKeyProvider keyProvider;

    public JjwtTokenValidator(EdDsaKeyProvider keyProvider){
        this.keyProvider = keyProvider;
    }

    @Override
    public AuthToken validate(String token) throws AuthenticationException {

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keyProvider.getPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new AuthToken(
                    claims.getSubject(),
                    claims.getExpiration().toInstant()
            );

        } catch (ExpiredJwtException e) {
            throw new AuthenticationException("Expired Token", Reason.TOKEN_EXPIRED);
        } catch (SignatureException e) {
            throw new AuthenticationException("Invalid Firm", Reason.TOKEN_INVALID);
        } catch (MalformedJwtException e) {
            throw new AuthenticationException("Malformed Token", Reason.TOKEN_MALFORMED);
        } catch (Exception e) {
            throw new AuthenticationException("Error validating Token", Reason.TOKEN_INVALID);
        }
    }
}
