package com.smartparking.payment_service.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtContextFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(JwtContextFilter.class);
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
        String path = http.getRequestURI();
        
        // Allow health endpoint without authentication
        if (path != null && path.endsWith("/health")) {
            chain.doFilter(request, response);
            return;
        }
        
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                JwtData data = jwtUtil.validateToken(auth);
                request.setAttribute(ATTR_CONTEXT, new RequestContext(data.getSubject(), data.getRole()));
                log.debug("JWT validated for path: {}, subject: {}, role: {}", path, data.getSubject(), data.getRole());
            } catch (JwtException | IllegalArgumentException e) {
                // Invalid or expired token - reject request
                log.warn("JWT validation failed for path: {}, error: {}", path, e.getMessage());
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            // No Authorization header - this is OK for health endpoints, but for others it will be rejected by controller
            log.debug("No Authorization header for path: {}", path);
        }
        chain.doFilter(request, response);
    }
}

