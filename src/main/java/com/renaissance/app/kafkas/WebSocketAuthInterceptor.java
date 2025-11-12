package com.renaissance.app.kafkas;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.renaissance.app.security.JwtProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
	
	@Autowired
	 private JwtProvider jwtConfig;

	 

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authToken = accessor.getFirstNativeHeader("Authorization");
            if (authToken != null && authToken.startsWith("Bearer ")) {
                String jwt = authToken.substring(7);
                // Validate JWT (reuse your existing logic)
                UsernamePasswordAuthenticationToken auth = validateJwt(jwt);
                if (auth != null) {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken validateJwt(String jwt) {
        try {
            // Your JWT validation logic
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtConfig.getSecretKey())
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();
            String username = claims.getSubject();
            return new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
        } catch (Exception e) {
            return null;
        }
    }
}
