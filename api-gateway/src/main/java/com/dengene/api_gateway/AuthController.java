package com.dengene.api_gateway;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // Dev-only: issues a token for any username with no password check.
    // A real system would verify credentials against a user store first.
    @PostMapping("/auth/token")
    public String getToken(@RequestParam String username) {
        return jwtUtil.generateToken(username);
    }
}