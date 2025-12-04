package com.smartparking.accounts_service.web;

import com.smartparking.accounts_service.service.AccountService;
import com.smartparking.accounts_service.security.JwtUtil;
import com.smartparking.accounts_service.web.dto.AuthRequest;
import com.smartparking.accounts_service.web.dto.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService svc;
    private final JwtUtil jwtUtil;

    public AuthController(AccountService svc, JwtUtil jwtUtil) { this.svc = svc; this.jwtUtil = jwtUtil; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        var acct = svc.register(req.getUsername(), req.getPassword());
        return ResponseEntity.ok(acct.getId());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        boolean ok = svc.verify(req.getUsername(), req.getPassword());
        if (!ok) return ResponseEntity.status(401).build();
        var acc = svc.findByUsername(req.getUsername()).get();
        String token = jwtUtil.generateToken(String.valueOf(acc.getId()), acc.getRole());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}

