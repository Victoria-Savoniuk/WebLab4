package com.example.WebLab3.controller;

import com.example.WebLab3.service.BinanceWebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CryptoWebSocketController {
    private final BinanceWebSocketService binanceWebSocketService;

    @MessageMapping("/subscribe")
    @SendTo("/topic/status")
    public String subscribe(List<String> symbols) {
        binanceWebSocketService.subscribeToSymbols(symbols);
        return "Subscribed to: " + String.join(", ", symbols);
    }

    @MessageMapping("/unsubscribe")
    @SendTo("/topic/status")
    public String unsubscribe() {
        binanceWebSocketService.unsubscribe();
        return "Unsubscribed from all symbols";
    }
}