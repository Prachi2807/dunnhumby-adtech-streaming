package com.dunnhumby.insights.model;

import java.time.Instant;

public record CampaignInsightResponse(
    String tenantId,
    String campaignId,
    String metric,
    long uniqueCustomers,
    Instant from,
    Instant to,
    boolean degradedFallback
) {}
