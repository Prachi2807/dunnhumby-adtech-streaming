package com.dunnhumby.insights.config;

import com.dunnhumby.insights.security.TenantAuthInterceptor;
import com.dunnhumby.insights.security.TenantRateLimiterInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantAuthInterceptor authInterceptor;
    private final TenantRateLimiterInterceptor rateLimiterInterceptor;

    public WebMvcConfig(TenantAuthInterceptor authInterceptor, TenantRateLimiterInterceptor rateLimiterInterceptor) {
        this.authInterceptor = authInterceptor;
        this.rateLimiterInterceptor = rateLimiterInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/ad/**");
        registry.addInterceptor(rateLimiterInterceptor).addPathPatterns("/ad/**");
    }
}
