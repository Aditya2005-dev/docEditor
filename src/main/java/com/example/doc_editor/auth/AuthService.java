package com.example.doc_editor.auth;

import com.example.doc_editor.auth.dto.LoginRequest;
import com.example.doc_editor.auth.dto.RegisterRequest;
import com.example.doc_editor.auth.dto.AuthResponse;
import com.example.doc_editor.security.JwtService;
import com.example.doc_editor.user.Role;
import com.example.doc_editor.user.User;
import com.example.doc_editor.user.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "Email already registered";
        }

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getName(),
                request.getEmail(),
                encodedPassword,
                Role.USER
        );

        userRepository.save(user);

        return "User registered successfully";
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            throw new RuntimeException("Invalid email or password");
        }

        String token =
                jwtService.generateToken(user.getEmail());

        return new AuthResponse(token);
    }
}