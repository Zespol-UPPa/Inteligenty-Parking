package com.smartparking.user_service.config;

import com.smartparking.user_service.security.JwtContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {
    @Bean
    public FilterRegistrationBean<JwtContextFilter> jwtFilterRegistration(JwtContextFilter filter) {
        FilterRegistrationBean<JwtContextFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/user/*");
        reg.setOrder(1);
        return reg;
    }
}


