package com.example.WebLab3.openidconnect;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenIdConnectProperties {
    @Value("${openidconnect.endpoint}")
    private String openIdConnectEndpoint;

    @Value("${openidconnect.clientId}")
    private String openIdConnectClientId;

    @Value("${openidconnect.clientSecret}")
    private String openIdConnectClientSecret;

    public String getOpenIdConnectEndpoint() {
        return openIdConnectEndpoint;
    }

    public String getOpenIdConnectClientId() {
        return openIdConnectClientId;
    }

    public String getOpenIdConnectClientSecret() {
        return openIdConnectClientSecret;
    }
} 