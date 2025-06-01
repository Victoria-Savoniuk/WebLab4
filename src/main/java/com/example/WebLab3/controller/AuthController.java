package com.example.WebLab3.controller;

import com.example.WebLab3.openidconnect.OpenIdConnectProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

@Controller
public class AuthController {

    private final OpenIdConnectProperties openIdConnectProperties;
    private final String redirectUri = "https://localhost:8080/lab3/callback";
    private final ConcurrentHashMap<String, Boolean> usedAuthCodes = new ConcurrentHashMap<>();

    @Autowired
    public AuthController(OpenIdConnectProperties openIdConnectProperties) {
        this.openIdConnectProperties = openIdConnectProperties;
    }

    private static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        
        @JsonProperty("id_token")
        private String idToken;
        
        @JsonProperty("refresh_token")
        private String refreshToken;
        
        @JsonProperty("token_type")
        private String tokenType;
        
        @JsonProperty("expires_in")
        private Integer expiresIn;
        
        @JsonProperty("scope")
        private String scope;

        public String getAccessToken() {
            return accessToken;
        }
    }

    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        String state = java.util.UUID.randomUUID().toString();
        
        String authorizeUrl = String.format("%s/login/oauth/authorize?" +
                "client_id=%s&response_type=code&redirect_uri=%s&scope=read&state=%s",
                openIdConnectProperties.getOpenIdConnectEndpoint(),
                openIdConnectProperties.getOpenIdConnectClientId(),
                redirectUri,
                state);
        System.out.println("+++++++++++++");
        response.sendRedirect(authorizeUrl);
    }

    @GetMapping("lab3/callback")
    public void callback(@RequestParam String code, HttpServletResponse response) throws IOException {
        System.out.println("method");
        try {
            if (usedAuthCodes.putIfAbsent(code, true) != null) {
                System.out.println("redirect to login");
                response.sendRedirect("/login");
                return;
            }

            disableSslVerification();
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "authorization_code");
            formData.add("code", code);
            formData.add("client_id", openIdConnectProperties.getOpenIdConnectClientId());
            formData.add("client_secret", openIdConnectProperties.getOpenIdConnectClientSecret());
            formData.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

            String tokenUrl = openIdConnectProperties.getOpenIdConnectEndpoint() + "/api/login/oauth/access_token";

            ResponseEntity<TokenResponse> tokenResponse = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    TokenResponse.class
            );

            if (tokenResponse.getBody() != null && tokenResponse.getBody().getAccessToken() != null) {
                String accessToken = tokenResponse.getBody().getAccessToken();
                
                Cookie cookie = new Cookie("access_token", accessToken);
                cookie.setPath("/");
                cookie.setHttpOnly(false);
                cookie.setSecure(false);
                cookie.setMaxAge(3600);
                
                response.addCookie(cookie);
                response.sendRedirect("/");
            } else {
                System.out.println("1");
                response.sendRedirect("/login");
            }
        } catch (HttpClientErrorException e) {
            System.out.println("2");
            response.sendRedirect("/login");
        } catch (Exception e) {
            System.out.println(e);
            response.sendRedirect("/login");
        }
    }

    public static void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 