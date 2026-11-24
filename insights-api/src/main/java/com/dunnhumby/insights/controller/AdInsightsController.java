package com.dunnhumby.insights.controller;

import com.dunnhumby.insights.model.CampaignInsightResponse;
import com.dunnhumby.insights.model.MetricType;
import com.dunnhumby.insights.security.TenantContext;
import com.dunnhumby.insights.service.AdInsightsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@RestController
@RequestMapping("/ad/{campaignID}")
public class AdInsightsController {

    private final AdInsightsService insightsService;

    public AdInsightsController(AdInsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/clicks")
    public ResponseEntity<CampaignInsightResponse> getClicks(
            @PathVariable("campaignID") String campaignId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return queryMetric(campaignId, MetricType.CLICK, from, to);
    }

    @GetMapping("/impressions")
    public ResponseEntity<CampaignInsightResponse> getImpressions(
            @PathVariable("campaignID") String campaignId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return queryMetric(campaignId, MetricType.IMPRESSION, from, to);
    }

    @GetMapping("/clickToBasket")
    public ResponseEntity<CampaignInsightResponse> getClickToBasket(
            @PathVariable("campaignID") String campaignId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return queryMetric(campaignId, MetricType.CLICK_TO_BASKET, from, to);
    }

    private ResponseEntity<CampaignInsightResponse> queryMetric(String campaignId, MetricType type, Instant from, Instant to) {
        Instant end = Optional.ofNullable(to).orElseGet(Instant::now);
        Instant start = Optional.ofNullable(from).orElseGet(() -> end.minus(24, ChronoUnit.HOURS));
        if (start.isAfter(end)) {
            return ResponseEntity.badRequest().build();
        }
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(insightsService.getCampaignMetric(tenantId, campaignId, type, start, end));
    }
}
