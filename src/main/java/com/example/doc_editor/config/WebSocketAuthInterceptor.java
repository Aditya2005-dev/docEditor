package com.example.doc_editor.config;

import com.example.doc_editor.security.JwtService;

import java.security.Principal;
import java.util.Collections;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

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


        // ==========================================
        // STOMP CONNECT
        // ==========================================

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            System.out.println(
                    "========== STOMP CONNECT =========="
            );


            String authorization =
                    accessor.getFirstNativeHeader(
                            "Authorization"
                    );


            System.out.println(
                    "STOMP AUTH HEADER: " +
                    (authorization != null)
            );


            if (
                    authorization == null ||
                    !authorization.startsWith("Bearer ")
            ) {

                System.out.println(
                        "STOMP AUTH FAILED"
                );

                return null;
            }


            String token =
                    authorization.substring(7);


            System.out.println(
                    "STOMP JWT VALID: " +
                    jwtService.isTokenValid(token)
            );


            if (!jwtService.isTokenValid(token)) {

                System.out.println(
                        "STOMP JWT INVALID"
                );

                return null;
            }


            String email =
                    jwtService.extractEmail(token);


            System.out.println(
                    "STOMP USER: " +
                    email
            );


            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.emptyList()
                    );


            /*
             * IMPORTANT:
             * Attach the authenticated user
             * to the STOMP session.
             */

            accessor.setUser(authentication);


            System.out.println(
                    "STOMP PRINCIPAL SET: " +
                    accessor.getUser()
            );

        }


        return message;
    }
}
