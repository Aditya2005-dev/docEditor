package com.example.doc_editor.config;

import com.example.doc_editor.security.JwtService;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(message);

        // Check only when client connects
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authorization =
                    accessor.getFirstNativeHeader("Authorization");

            // No JWT
            if (authorization == null
                    || !authorization.startsWith("Bearer ")) {

                return null;
            }

            // Remove "Bearer "
            String token =
                    authorization.substring(7);

            // Invalid JWT
            if (!jwtService.isTokenValid(token)) {
                return null;
            }

            // Get email from JWT
            String email =
                    jwtService.extractEmail(token);

            // Create authenticated user
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.emptyList()
                    );

            // Attach user to WebSocket session
            accessor.setUser(authentication);
        }

        return message;
    }
}