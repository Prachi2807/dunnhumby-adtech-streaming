package com.dunnhumby.insights.repository;

import com.dunnhumby.insights.model.MetricType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class ClickHouseInsightsRepository {

    private final JdbcTemplate jdbcTemplate;

    public ClickHouseInsightsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long fetchUniqueCustomerCount(String tenantId, String campaignId, MetricType metricType, Instant from, Instant to) {
        String sql = """
            SELECT uniqMerge(unique_users_state)
            FROM adtech.campaign_hourly_stats
            WHERE tenant_id = ?
              AND campaign_id = ?
              AND event_type = ?
              AND event_hour >= toStartOfHour(?)
              AND event_hour <= toStartOfHour(?)
            """;

        Long result = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                tenantId,
                campaignId,
                metricType.name(),
                Timestamp.from(from),
                Timestamp.from(to)
        );
        return result != null ? result : 0L;
    }
}
