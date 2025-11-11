package com.smartparking.api_gateway.filter;

import com.smartparking.security.JwtData;
import com.smartparking.security.JwtUtil;
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
        return path.startsWith("/auth") || path.startsWith("/health") || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            JwtData data = jwtUtil.validateToken(auth);
            req.setAttribute(ATTR_JWT, data);

            // 🔹 Tworzymy listę ról (Spring wymaga prefiksu ROLE_)
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + data.getRole().name()));

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
        } catch (SecurityException ex) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
