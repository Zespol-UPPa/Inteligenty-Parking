package com.smartparking.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;


public class InternalAuthFilter implements Filter {
    public static final String HEADER_SUBJECT = "X-User-Subject";
    public static final String HEADER_ROLE = "X-User-Role";
    public static final String ATTR_CONTEXT = "requestContext";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;
        String subject = http.getHeader(HEADER_SUBJECT);
        String role = http.getHeader(HEADER_ROLE);

        if (subject != null && role != null) {
            RequestContext context = new RequestContext(subject, role);
            req.setAttribute(ATTR_CONTEXT, context);
        }
        chain.doFilter(req, res);
    }
}

