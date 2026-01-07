package com.smartparking.api_gateway.filter;

import com.smartparking.api_gateway.security.JwtData;
import com.smartparking.api_gateway.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public static final String ATTR_JWT = "jwtData";

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) return false;
        // Allow authentication endpoints and actuator
        if (path.startsWith("/auth") || path.startsWith("/api/auth") || path.startsWith("/actuator")) return true;
        // Allow any health endpoint (e.g. /health or /payment/health, /customer/health)
        if (path.equals("/health") || path.endsWith("/health")) return true;
        // Allow public parking endpoints (locations, spots, details, occupancy, pricing)
        if (path.equals("/parking/locations") || path.equals("/parking/spots")) return true;
        if (path.startsWith("/parking/locations/") && (path.endsWith("/details") || path.endsWith("/occupancy"))) return true;
        if (path.startsWith("/parking/pricing/") && path.endsWith("/reservation-fee")) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // Try to get token from Authorization header first (for backward compatibility)
        String auth = req.getHeader("Authorization");
        String token = null;
        
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        } else {
            // Try to get token from cookie (fallback for cookie-based auth)
            jakarta.servlet.http.Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("authToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }
        
        if (token == null || token.isBlank()) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            // JwtUtil.validateToken expects "Bearer " prefix
            JwtData data = jwtUtil.validateToken("Bearer " + token);
            req.setAttribute(ATTR_JWT, data);

            // 🔹 Tworzymy listę ról (Spring wymaga prefiksu ROLE_)
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + data.getRole()));

            // 🔹 Tworzymy Authentication z danymi z JWT
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            data.getSubject(), // login lub e-mail
                            null,
                            authorities
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

            // 🔹 Ustawiamy użytkownika w kontekście Spring Security
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(req, res);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
