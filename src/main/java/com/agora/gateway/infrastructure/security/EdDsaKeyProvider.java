package com.agora.gateway.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class EdDsaKeyProvider {

    @Value("${auth.public-key}")
    private String publicKeyBase64;

    private PublicKey cachedKey;

    public PublicKey getPublicKey(){
        if(cachedKey == null){
            cachedKey = buildPublicKey();
        }
        return cachedKey;
    }

    private PublicKey buildPublicKey(){

        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EdDSA");
            return keyFactory.generatePublic(keySpec);
        } catch(Exception e){

            throw new IllegalStateException("Public key not loaded. " + "Verify the public key");

        }

    }

}
