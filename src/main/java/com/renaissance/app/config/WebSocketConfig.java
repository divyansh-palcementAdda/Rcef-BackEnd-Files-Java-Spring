package com.renaissance.app.config;

import com.renaissance.app.kafkas.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    // ==============================================================
    // 1. MESSAGE BROKER + HEARTBEAT
    // ==============================================================
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(taskScheduler())           // REQUIRED
                .setHeartbeatValue(new long[]{10000, 10000}); // 10s send, 10s recv

        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    // ==============================================================
    // 2. STOMP ENDPOINT (WebSocket entry)
    // ==============================================================
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notifications")
                .setAllowedOriginPatterns(
                        "http://localhost:4200",
                        "http://192.168.0.183:4200",
                        "https://yourdomain.com" // Add prod later
                )
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setDisconnectDelay(30_000); // 30s
    }

    // ==============================================================
    // 3. INBOUND AUTH INTERCEPTOR (JWT Validation)
    // ==============================================================
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }

    // ==============================================================
    // 4. TASK SCHEDULER (REQUIRED FOR HEARTBEAT)
    // ==============================================================
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("wss-heartbeat-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}