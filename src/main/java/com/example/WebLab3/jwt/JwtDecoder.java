package com.example.WebLab3.jwt;

import com.example.WebLab3.openidconnect.OpenIdConnectProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtDecoder {
    
    private final OpenIdConnectProperties openIdConnectProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public JwtDecoder(OpenIdConnectProperties openIdConnectProperties) {
        this.openIdConnectProperties = openIdConnectProperties;
    }

    public Claims decodeToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Invalid token format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            Claims finalClaims = Jwts.claims();
            
            String sub = (String) claims.getOrDefault("sub", 
                        claims.getOrDefault("id",
                        claims.getOrDefault("user_id", 
                        claims.getOrDefault("preferred_username", "unknown"))));
            
            finalClaims.setSubject(sub);
            
            String name = (String) claims.getOrDefault("name",
                         claims.getOrDefault("preferred_username",
                         claims.getOrDefault("username",
                         claims.getOrDefault("display_name", sub))));
            
            finalClaims.put("name", name);
            
            claims.forEach((key, value) -> {
                if (!key.equals("sub") && !key.equals("name")) {
                    finalClaims.put(key, value);
                }
            });
            
            return finalClaims;
        } catch (Exception e) {
            throw new RuntimeException("Token decoding error: " + e.getMessage(), e);
        }
    }
} 