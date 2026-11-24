CREATE DATABASE IF NOT EXISTS adtech;

CREATE TABLE IF NOT EXISTS adtech.ad_events_enriched (
    event_id String,
    tenant_id LowCardinality(String),
    campaign_id String,
    event_type LowCardinality(String),
    user_id String,
    event_time DateTime64(3)
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(event_time)
ORDER BY (tenant_id, campaign_id, event_type, event_time)
TTL toDateTime(event_time) + INTERVAL 90 DAY;

CREATE TABLE IF NOT EXISTS adtech.campaign_hourly_stats (
    tenant_id LowCardinality(String),
    campaign_id String,
    event_type LowCardinality(String),
    event_hour DateTime,
    unique_users_state AggregateFunction(uniq, String)
)
ENGINE = AggregatingMergeTree()
PARTITION BY toYYYYMM(event_hour)
ORDER BY (tenant_id, campaign_id, event_type, event_hour);

CREATE MATERIALIZED VIEW IF NOT EXISTS adtech.mv_campaign_hourly_stats
TO adtech.campaign_hourly_stats AS
SELECT
    tenant_id,
    campaign_id,
    event_type,
    toStartOfHour(event_time) AS event_hour,
    uniqState(user_id) AS unique_users_state
FROM adtech.ad_events_enriched
GROUP BY tenant_id, campaign_id, event_type, event_hour;

INSERT INTO adtech.ad_events_enriched VALUES
    ('e1', 'flipkart', 'camp_101', 'IMPRESSION', 'u_101', now() - INTERVAL 15 MINUTE),
    ('e2', 'flipkart', 'camp_101', 'CLICK', 'u_101', now() - INTERVAL 14 MINUTE),
    ('e3', 'flipkart', 'camp_101', 'CLICK_TO_BASKET', 'u_101', now() - INTERVAL 5 MINUTE),
    ('e4', 'flipkart', 'camp_101', 'IMPRESSION', 'u_202', now() - INTERVAL 12 MINUTE),
    ('e5', 'flipkart', 'camp_101', 'CLICK', 'u_202', now() - INTERVAL 10 MINUTE);
