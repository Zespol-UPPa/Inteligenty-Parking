package com.smartparking.worker_service.security;

import com.smartparking.security.JwtData;
import com.smartparking.security.JwtUtil;
import com.smartparking.security.RequestContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
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
        String auth = http.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                JwtData data = jwtUtil.validateToken(auth);
                request.setAttribute(ATTR_CONTEXT, new RequestContext(data.getSubject(), data.getRole().name()));
            } catch (Exception ignored) {
            }
        }
        chain.doFilter(request, response);
    }
}


