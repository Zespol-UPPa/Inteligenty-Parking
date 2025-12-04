package com.smartparking.accounts_service.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtContextFilter implements Filter {
    private final JwtUtil jwtUtil;
    public static final String ATTR_CONTEXT = "requestContext";

    public JwtContextFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String auth = http.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                JwtData data = jwtUtil.validateToken(auth);
                request.setAttribute(ATTR_CONTEXT, new RequestContext(data.getSubject(), data.getRole()));
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                // Invalid or expired token - reject request
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

