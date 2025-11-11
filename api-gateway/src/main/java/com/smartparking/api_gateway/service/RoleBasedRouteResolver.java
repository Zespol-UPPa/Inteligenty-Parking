package com.smartparking.api_gateway.service;


import com.smartparking.security.Role;
import com.smartparking.api_gateway.config.RoutesProperties;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedRouteResolver implements RouteResolver {

    private final RoutesProperties routes;

    public RoleBasedRouteResolver(RoutesProperties routes) {
        this.routes = routes;
    }

    @Override
    public String resolveBaseUrl(Role role, String path) {
        // Route based on path prefix
        if (path.startsWith("/user/")) {
            return routes.getUserService();
        } else if (path.startsWith("/payment/")) {
            return routes.getPaymentService();
        } else if (path.startsWith("/parking/")) {
            return routes.getParkingService();
        } else if (path.startsWith("/admin/")) {
            return routes.getAdminService();
        } else if (path.startsWith("/worker/")) {
            return routes.getWorkerService();
        }
        return null; // 404 dla wszystkiego innego
    }
}
