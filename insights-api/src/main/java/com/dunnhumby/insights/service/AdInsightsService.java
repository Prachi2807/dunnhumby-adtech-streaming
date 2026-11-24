package com.dunnhumby.insights.service;

import com.dunnhumby.insights.model.CampaignInsightResponse;
import com.dunnhumby.insights.model.MetricType;
import com.dunnhumby.insights.repository.ClickHouseInsightsRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AdInsightsService {

    private static final Logger log = LoggerFactory.getLogger(AdInsightsService.class);
    private final ClickHouseInsightsRepository repository;

    public AdInsightsService(ClickHouseInsightsRepository repository) {
        this.repository = repository;
    }

    @CircuitBreaker(name = "clickhouseQuery", fallbackMethod = "fallbackInsight")
    public CampaignInsightResponse getCampaignMetric(String tenantId, String campaignId, MetricType metric, Instant from, Instant to) {
        long count = repository.fetchUniqueCustomerCount(tenantId, campaignId, metric, from, to);
        return new CampaignInsightResponse(tenantId, campaignId, metric.name(), count, from, to, false);
    }

    @SuppressWarnings("unused")
    private CampaignInsightResponse fallbackInsight(String tenantId, String campaignId, MetricType metric, Instant from, Instant to, Throwable ex) {
        log.error("ClickHouse query failed for tenant={} campaign={} metric={}: {}", tenantId, campaignId, metric, ex.getMessage());
        return new CampaignInsightResponse(tenantId, campaignId, metric.name(), -1L, from, to, true);
    }
}
