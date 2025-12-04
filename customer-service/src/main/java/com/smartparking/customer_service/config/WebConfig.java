package com.smartparking.customer_service.config;

import com.smartparking.customer_service.security.JwtContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {
    @Bean
    public FilterRegistrationBean<JwtContextFilter> jwtFilter(JwtContextFilter filter) {
        FilterRegistrationBean<JwtContextFilter> bean = new FilterRegistrationBean<>(filter);
        bean.addUrlPatterns("/customer/*");
        bean.setOrder(1);
        return bean;
    }
}

