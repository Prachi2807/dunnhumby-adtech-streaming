package com.dunnhumby.insights.observability;

import com.dunnhumby.insights.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = Optional.ofNullable(request.getHeader("X-Trace-Id"))
                .orElse(UUID.randomUUID().toString());
        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);

        long startMs = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long latencyMs = System.currentTimeMillis() - startMs;
            log.info("{\"traceId\":\"{}\",\"tenantId\":\"{}\",\"method\":\"{}\",\"path\":\"{}\",\"status\":{},\"latencyMs\":{}}",
                    traceId, TenantContext.getTenantId(), request.getMethod(),
                    request.getRequestURI(), response.getStatus(), latencyMs);
            MDC.clear();
        }
    }
}
