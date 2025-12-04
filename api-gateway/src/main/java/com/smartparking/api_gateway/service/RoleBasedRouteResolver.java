package com.smartparking.api_gateway.service;



import com.smartparking.api_gateway.security.Role;
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
        // Normalize path: allow /api/* prefix as well
        if (path == null) return null;
        String p = path;
        if (p.startsWith("/api/")) {
            p = p.substring(4); // turn "/api/ocr/..." into "/ocr/..."
        }

        // Route based on path prefix
        if (p.startsWith("/customer/")) {
            return routes.getCustomerService();
        } else if (p.startsWith("/payment/")) {
            return routes.getPaymentService();
        } else if (p.startsWith("/parking/")) {
            return routes.getParkingService();
        } else if (p.startsWith("/admin/")) {
            return routes.getAdminService();
        } else if (p.startsWith("/worker/")) {
            return routes.getWorkerService();
        } else if (p.startsWith("/accounts/")) {
            return routes.getAccountsService();
        } else if (p.startsWith("/company/")) {
            return routes.getCompanyService();
        } else if (p.startsWith("/ocr/")) {
            return routes.getOcrService();
        }
        return null; // 404 dla wszystkiego innego
    }
}
