package com.dunnhumby.insights.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TenantRateLimiterInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_SECOND = 100;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = TenantContext.getTenantId();
        long currentSecond = Instant.now().getEpochSecond();

        WindowCounter counter = counters.compute(tenantId, (k, existing) -> {
            if (existing == null || existing.epochSecond != currentSecond) {
                return new WindowCounter(currentSecond, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter.count.get() > MAX_REQUESTS_PER_SECOND) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Rate limit exceeded for tenant: " + tenantId);
            return false;
        }
        return true;
    }

    private record WindowCounter(long epochSecond, AtomicInteger count) {}
}
