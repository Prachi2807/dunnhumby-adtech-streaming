package com.dunnhumby.insights.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class TenantAuthInterceptor implements HandlerInterceptor {

    private static final Map<String, String> API_KEY_TO_TENANT = Map.of(
        "flipkart-secret-key", "flipkart",
        "walmart-secret-key", "walmart",
        "target-secret-key", "target"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader("X-API-Key");
        String tenantId = (apiKey != null) ? API_KEY_TO_TENANT.get(apiKey) : null;

        if (tenantId == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing or invalid X-API-Key");
            return false;
        }
        TenantContext.setTenantId(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
