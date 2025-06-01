package com.example.WebLab3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BinanceWebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private WebSocketClient webSocketClient;
    private final List<String> subscribedSymbols = new ArrayList<>();

    public BinanceWebSocketService(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void subscribeToSymbols(List<String> symbols) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }

        String streams = symbols.stream()
                .map(symbol -> symbol.toLowerCase() + "@ticker")
                .collect(java.util.stream.Collectors.joining("/"));
        String wsUrl = "wss://fstream.binance.com/ws/" + streams;

        try {
            webSocketClient = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    log.info("Connected to Binance WebSocket");
                }

                @Override
                public void onMessage(String message) {
                    try {
                        messagingTemplate.convertAndSend("/topic/crypto", message);
                    } catch (Exception e) {
                        log.error("Error processing message: {}", e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("Connection closed: {} {}", code, reason);
                }

                @Override
                public void onError(Exception ex) {
                    log.error("WebSocket error: {}", ex.getMessage());
                }
            };

            webSocketClient.connect();
            subscribedSymbols.clear();
            subscribedSymbols.addAll(symbols);
        } catch (Exception e) {
            log.error("Error connecting to Binance WebSocket: {}", e.getMessage());
        }
    }

    public void unsubscribe() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }
        subscribedSymbols.clear();
    }

    public List<String> getSubscribedSymbols() {
        return new ArrayList<>(subscribedSymbols);
    }
} 