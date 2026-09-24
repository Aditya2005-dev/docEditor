package com.example.doc_editor.auth;

import com.example.doc_editor.auth.dto.AuthResponse;
import com.example.doc_editor.auth.dto.LoginRequest;
import com.example.doc_editor.auth.dto.RegisterRequest;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(
            @RequestBody RegisterRequest request
    ) {

        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody LoginRequest request
    ) {

        return authService.login(request);
    }

    @GetMapping("/hello")
    public String hello() {

        return "You are authenticated!";
    }
}