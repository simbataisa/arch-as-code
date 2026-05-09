# Observability Standards Best Practice

Status: Approved | Last Reviewed: 2026-03-06 | Owner: @ea-board
Catalog ID: BP-004 | Radii
Tier Applicability: T0, T1, T2, T3

## Problem Statement

Without observability, production issues are invisible:

- "System is slow" — where? payment? orders? database?
- Can't find root cause of failures
- Debugging production is guesswork
- Performance issues go unnoticed until customers complain
- Security incidents detected too late

## Solution

Implement Three Pillars of Observability: Logs, Metrics, Traces. Combined, they provide complete visibility.

## Three Pillars of Observability

### 1. Logs (Events)

**Purpose**: Detailed record of what happened.

**Example Log Entry**:

```json
{
  "timestamp": "2026-03-08T10:30:15.123Z",
  "level": "INFO",
  "logger": "com.techcombank.order.OrderService",
  "message": "Order created successfully",
  "traceId": "abc123def456",
  "spanId": "span789",
  "service": "order-service",
  "version": "1.2.3",
  "environment": "production",
  "userId": "user_456",
  "orderId": "ord_789",
  "amount": 50000.0,
  "currency": "VND",
  "duration_ms": 234
}
```

**Best Practices**:

- Structured JSON (machine-readable)
- Include: timestamp, level, service, traceId, spanId, userId, context
- Log at appropriate levels:
  - ERROR: Errors requiring immediate attention
  - WARN: Suspicious but not failing (slow query, retry)
  - INFO: Important business events (order created, payment processed)
  - DEBUG: Detailed for troubleshooting (variable values, method entry/exit)
- Avoid: PII (passwords, credit cards), large payloads

```java
@Service
public class OrderService {
  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  public Order createOrder(CreateOrderRequest request) {
    log.info("Creating order for customer",
      kv("customerId", request.getCustomerId()),
      kv("amount", request.getAmount()),
      kv("currency", request.getCurrency())
    );

    try {
      Order order = orderRepository.save(...);
      log.info("Order created successfully",
        kv("orderId", order.getId()),
        kv("duration_ms", timer.elapsed(TimeUnit.MILLISECONDS))
      );
      return order;

    } catch (Exception e) {
      log.error("Failed to create order", e,
        kv("customerId", request.getCustomerId()),
        kv("exception", e.getClass().getSimpleName())
      );
      throw e;
    }
  }
}
```

**Log Storage**:

- ELK Stack (Elasticsearch, Logstash, Kibana)
- Splunk
- CloudWatch (AWS)
- Stackdriver (GCP)
- Azure Monitor

### 2. Metrics (Time Series)

**Purpose**: Quantifiable measurements aggregated over time.

**Example Metrics**:

```
# Request latency
order_service_request_duration_seconds{service="order-service",endpoint="/api/v1/orders",method="POST"} 0.234
order_service_request_duration_seconds{service="order-service",endpoint="/api/v1/orders",method="GET"} 0.045

# Error rate
order_service_requests_total{service="order-service",status="200"} 9500
order_service_requests_total{service="order-service",status="500"} 50

# Business metrics
orders_created_total{service="order-service"} 25000
orders_amount_total{service="order-service",currency="VND"} 1250000000
```

**Prometheus Format** (Standard):

```
# HELP request_duration_seconds HTTP request latency
# TYPE request_duration_seconds histogram
request_duration_seconds_bucket{le="0.1"} 100
request_duration_seconds_bucket{le="0.5"} 500
request_duration_seconds_bucket{le="1.0"} 950
request_duration_seconds_bucket{le="+Inf"} 1000
request_duration_seconds_sum 234.5
request_duration_seconds_count 1000
```

**Java Implementation**:

```java
@Configuration
public class MetricsConfig {
  @Bean
  public MeterRegistry meterRegistry() {
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }
}

@Service
public class OrderService {
  @Autowired
  private MeterRegistry meterRegistry;

  public Order createOrder(CreateOrderRequest request) {
    Timer.Sample sample = Timer.start(meterRegistry);

    try {
      Order order = orderRepository.save(...);

      // Record success metrics
      meterRegistry.counter("orders.created",
        "currency", order.getCurrency(),
        "status", "success"
      ).increment();

      meterRegistry.gauge("order.amount",
        order.getAmount().doubleValue()
      );

      return order;

    } finally {
      sample.stop(Timer.builder("order.creation.duration")
        .publishPercentiles(0.5, 0.95, 0.99)
        .record());
    }
  }
}
```

**Key Metrics to Track**:

- Request latency (p50, p95, p99)
- Error rate (5xx, timeouts)
- Throughput (requests/sec)
- Resource utilization (CPU, memory, disk, connections)
- Business metrics (orders created, transactions, revenue)
- Cache hit rate
- Database query latency
- Queue depth

**Tools**:

- Prometheus (scrapes metrics)
- Grafana (visualization)
- CloudWatch (AWS)
- Stackdriver (GCP)

### 3. Traces (Distributed Tracing)

**Purpose**: Follow a request through multiple services.

**Example trace** for `POST /api/v1/orders`:

```mermaid
gantt
    title Distributed trace — POST /api/v1/orders (total 234 ms)
    dateFormat X
    axisFormat %L
    section Order Service
    Validate Order        :0, 10
    Save Order            :10, 50
    Call Payment Service  :50, 200
    Call Inventory Service:200, 234
    section Payment Service
    Validate Payment      :50, 60
    Charge Card           :60, 180
    Save Transaction      :180, 200
    section Inventory Service
    Reserve Items         :200, 220
    Update Stock          :220, 234
```

**Java Implementation** (Spring Cloud Sleuth + Jaeger):

```java
// Spring auto-injects traceId and spanId
@Service
public class OrderService {
  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  @Autowired
  private PaymentServiceClient paymentClient;

  @Autowired
  private InventoryServiceClient inventoryClient;

  @Autowired
  @Qualifier("tracer")
  private io.opentelemetry.api.trace.Tracer tracer;

  public Order createOrder(CreateOrderRequest request) {
    try (Scope scope = tracer.spanBuilder("create_order")
        .setAttribute("customer_id", request.getCustomerId())
        .startScope()) {

      // traceId is automatically in all logs
      log.info("Creating order");

      // Process payment (creates child span)
      PaymentResult payment = processPayment(request);

      // Reserve inventory (creates child span)
      reserveInventory(request);

      Order order = orderRepository.save(...);
      return order;
    }
  }

  private PaymentResult processPayment(CreateOrderRequest request) {
    try (Scope scope = tracer.spanBuilder("process_payment")
        .setAttribute("amount", request.getAmount())
        .startScope()) {
      return paymentClient.charge(request);
    }
  }
}
```

**Tools**:

- Jaeger (open-source)
- Zipkin (open-source)
- DataDog (SaaS)
- Lightstep (SaaS)
- X-Ray (AWS)

## Implementation Standards

### Log Configuration

```yaml
# application.yml
logging:
  level:
    root: INFO
    com.techcombank: INFO
    org.springframework: WARN
    org.hibernate: WARN

  pattern:
    console: "%d{ISO8601} [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n"
    file: "%d{ISO8601} [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n"

  file:
    name: /var/log/application/app.log
    max-size: 100MB
    max-history: 10
    total-size-cap: 1GB

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

### Metrics Configuration

```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5,0.95,0.99,0.999

spring:
  application:
    name: order-service
    version: 1.2.3

  cloud:
    consul:
      discovery:
        enabled: true
        instanceId: order-service-${spring.application.instance-id:${random.value}}
```

### Trace Configuration

```yaml
spring:
  cloud:
    sleuth:
      enabled: true
      propagation-keys: traceId,spanId,customerId
      sampler:
        probability: 0.1 # Sample 10% (reduce overhead)
        # Always sample these:
        rules:
          - { path: /api/v1/payments.*, probability: 1.0 }
          - { path: /api/v1/orders.*, probability: 0.5 }

    zipkin:
      base-url: http://zipkin:9411
      sender:
        type: web
```

## SLI/SLO Definition

**SLI** (Service Level Indicator): Measurable metric
**SLO** (Service Level Objective): Target for SLI

```
SLO: 99.5% availability

SLIs:
  - Request latency (p99) < 500ms
  - Error rate < 0.5%
  - Data freshness < 1 minute
  - System availability > 99.5% (uptime)

Example SLO:
  "Order Service will respond to 99.5% of requests within 500ms"
  "Payment Service error rate will be < 0.1%"
```

## Alerting

```yaml
# Prometheus alerting rules
groups:
  - name: order-service
    rules:
      - alert: HighLatency
        expr: histogram_quantile(0.99, http_request_duration_seconds) > 1
        for: 5m
        annotations:
          summary: "High latency detected"

      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.01
        for: 2m
        annotations:
          summary: "Error rate > 1%"

      - alert: DatabaseConnectionPoolExhausted
        expr: db_pool_available < 5
        for: 1m
        annotations:
          summary: "DB connection pool exhausted"
```

## Observability Checklist

- [ ] All services emit structured JSON logs
- [ ] Logs include traceId, spanId, userId, service, version
- [ ] Metrics exported in Prometheus format
- [ ] Traces propagated across services (OpenTelemetry standard)
- [ ] SLIs/SLOs defined per service
- [ ] Alerts configured for SLO violations
- [ ] Dashboards for key metrics (latency, error rate, throughput)
- [ ] Runbooks for common alerts
- [ ] Log retention: 30 days minimum
- [ ] Metrics retention: 15 days minimum

## References

- [Three Pillars of Observability](https://www.oreilly.com/library/view/observability-engineering/9781492076438/)
- [OpenTelemetry](https://opentelemetry.io/)
- [Prometheus](https://prometheus.io/)
- [Jaeger Tracing](https://www.jaegertracing.io/)
- [Google SRE Book - Monitoring](https://sre.google/books/)

---

**Key Takeaway**: Implement logs (JSON structured), metrics (Prometheus), and traces (Jaeger/Zipkin). Define SLIs/SLOs. Alert on deviations. Correlate across three pillars for root cause analysis.
