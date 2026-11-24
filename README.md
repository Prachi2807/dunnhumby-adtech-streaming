# Real-Time Retail Media & Ad-Tech Streaming Platform (dunnhumby Case Study)

## 1. Executive Summary
A multi-tenant, low-latency streaming analytics platform designed for retail media networks. It ingests high-velocity shopper interactions (`IMPRESSION`, `CLICK`, `ADD_TO_CART`), performs stateful cross-stream attribution in **Apache Flink**, stores real-time and historical rollups in **ClickHouse**, and exposes sub-50ms **Insights APIs** in **Java 21 / Spring Boot**.

## 2. End-to-End Architecture & Data Flow

```mermaid
flowchart LR
    subgraph Client["Retailer Websites (Multi-Tenant)"]
        Web["Web / Mobile SDK<br/>(Batched Impressions, Beacon Clicks)"]
    end

    subgraph Edge["Edge & Ingestion Layer"]
        WAF["WAF + DDoS"]
        GW["API Gateway<br/>(TLS 1.3, JWT/API Key, Rate Limit)"]
        Ingest["Ingestion Service<br/>(Schema Validation, PII Hashing)"]
        Kafka[("Apache Kafka<br/>3 Topics: ad-impressions, ad-clicks, cart-events<br/>Partitioned by tenantId:userId")]
        DLQ[("Dead Letter Queue")]
    end

    subgraph Compute["Stateful Stream Processing (Apache Flink)"]
        Flink["Flink TaskManagers<br/>(keyBy tenantId:userId, Event-Time Watermarks)"]
        RocksDB[("Embedded RocksDB<br/>24h TTL Click State + Dedup")]
        S3_CP[("Amazon S3<br/>Incremental Checkpoints")]
    end

    subgraph Storage["Unified OLAP & Tiered Storage (ClickHouse)"]
        Base[("ad_events_enriched (MergeTree)<br/>Hot NVMe -> S3 Cold TTL")]
        MV["Materialized View<br/>(uniqState HyperLogLog Sketches)"]
        Rollup[("campaign_hourly_stats<br/>(AggregatingMergeTree)")]
    end

    subgraph Serving["Insights API (Java 21 / Spring Boot)"]
        API["GET /ad/{campaignID}/clicks<br/>GET /ad/{campaignID}/impressions<br/>GET /ad/{campaignID}/clickToBasket"]
    end

    Web -->|HTTPS| WAF --> GW --> Ingest --> Kafka
    Ingest -.->|Poison Events| DLQ
    Kafka --> Flink
    Flink <-->|Microsecond State| RocksDB
    RocksDB -.->|Async Snapshot| S3_CP
    Flink -->|Micro-Batch 500ms| Base
    Base -->|Insert Trigger| MV --> Rollup
    Rollup -->|uniqMerge <20ms| API
```

## 3. Cloud Deployment Topology (Multi-AZ Kubernetes)

```mermaid
flowchart TB
    Internet((Shoppers & Marketers)) --> ALB["AWS ALB / API Gateway (Multi-AZ)"]
    subgraph VPC["Production VPC (3 Availability Zones)"]
        subgraph EKS["Amazon EKS (Kubernetes Cluster)"]
            IngestPods["Ingestion Pods (HPA: CPU/QPS)"]
            FlinkTM["Flink TaskManagers (KEDA: Kafka Consumer Lag)"]
            ApiPods["Spring Boot Insights API Pods (HPA)"]
        end
        subgraph DataPlane["Managed Stateful Services (Private Subnets)"]
            MSK[("Amazon MSK (Kafka)<br/>RF=3, min.insync.replicas=2")]
            CH[("ClickHouse Cluster<br/>Sharded + Replicated + S3 Tiered Disk")]
        end
    end
    ALB --> IngestPods & ApiPods
    IngestPods --> MSK --> FlinkTM --> CH
    ApiPods --> CH
```

## 4. Cross-Cutting Concerns
* **Security & Multi-Tenancy:** TLS 1.3 in transit; KMS encryption at rest; `X-API-Key` / JWT authentication extracting `tenant_id` into request scope; `tenant_id` enforced in every Kafka partition key and ClickHouse `WHERE` predicate.
* **Observability:** Correlation ID (`X-Trace-Id`) propagation, structured JSON access logs, Spring Boot Actuator health probes (`/actuator/health`), and Prometheus metrics (`/actuator/prometheus`).
* **Scaling & Partitioning:** Kafka topics partitioned by `tenantId:userId`; KEDA scales Flink TaskManagers on Kafka consumer lag; HPA scales API pods on QPS.
* **Resilience:** Kafka `acks=all`, Flink incremental S3 checkpoints (exactly-once), DLQ for malformed events, and Resilience4j Circuit Breaker + fallback on ClickHouse reads.

## 5. Quick Start
```bash
docker-compose up --build -d
curl -H "X-API-Key: flipkart-secret-key" "http://localhost:8080/ad/camp_101/impressions"
curl -H "X-API-Key: flipkart-secret-key" "http://localhost:8080/ad/camp_101/clicks"
curl -H "X-API-Key: flipkart-secret-key" "http://localhost:8080/ad/camp_101/clickToBasket"
```
