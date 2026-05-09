# API Gateway Pattern

Status: Approved | Last Reviewed: 2026-02-08 | Owner: @ea-board
Catalog ID: INT-003 | Radii
Tier Applicability: T0, T1, T2, T3

## Problem Statement

Multiple microservices create operational challenges for clients:
- Clients must discover and manage connections to each service
- Duplicate cross-cutting concerns: authentication, rate limiting, logging, routing
- Service locations change frequently (auto-scaling, deployments)
- Complex networks expose internal services unnecessarily
- Difficult to evolve APIs without breaking clients

## Solution

Implement a single entry point (API Gateway) that handles routing, auth, rate limiting, and protocol conversion.

```
┌─────────┐  ┌──────────────┐  ┌──────────────┐
│ Client  │──│ API Gateway  │──│ Order Service│
└─────────┘  │              │  └──────────────┘
             │ - Auth       │
             │ - Rate limit │  ┌──────────────┐
             │ - Routing    │──│Payment Service
             │ - Logging    │  └──────────────┘
             │ - Caching    │
             └──────────────┘  ┌──────────────┐
                               │Inventory Svc │
                               └──────────────┘
```

## Implementation Guidelines

1. **Choose API Gateway**
   - **Kong**: Popular, open-source, Kubernetes-ready
   - **Spring Cloud Gateway**: Spring Boot native, lightweight
   - **AWS API Gateway**: Managed service, tight AWS integration
   - **Nginx**: Simple, high-performance, familiar

2. **Spring Cloud Gateway Example**
   ```yaml
   spring:
     cloud:
       gateway:
         routes:
           # Order Service Routes
           - id: order-service
             uri: http://order-service:8080
             predicates:
               - Path=/api/v1/orders/**
             filters:
               - RewritePath=/api/v1/orders/(?<segment>.*), /orders/$\{segment}
               - AddRequestHeader=X-Service-Name, order-service

           # Payment Service Routes
           - id: payment-service
             uri: http://payment-service:8081
             predicates:
               - Path=/api/v1/payments/**
             filters:
               - RewritePath=/api/v1/payments/(?<segment>.*), /payments/$\{segment}

           # Inventory Service Routes
           - id: inventory-service
             uri: http://inventory-service:8082
             predicates:
               - Path=/api/v1/inventory/**
             filters:
               - RewritePath=/api/v1/inventory/(?<segment>.*), /inventory/$\{segment}

         # Global filters (apply to all routes)
         default-filters:
           - name: RequestRateLimiter
             args:
               redis-rate-limiter:
                 replenishRate: 100  # requests per second
                 burstCapacity: 200
               key-resolver: "#{@ipAddressKeyResolver}"
           - name: CircuitBreaker
             args:
               name: myCircuitBreaker
               fallback: forward:/fallback

   # Authentication
   security:
     oauth2:
       resourceserver:
         jwt:
           issuer-uri: https://auth-server.techcombank.com
   ```

3. **Custom Rate Limiter**
   ```java
   @Component
   public class IpAddressKeyResolver implements KeyResolver {

     @Override
     public Mono<String> resolve(ServerWebExchange exchange) {
       return Mono.just(
         exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
       );
     }
   }

   @Configuration
   public class GatewayConfig {

     @Bean
     public RouteLocator customRoutes(RouteLocatorBuilder builder) {
       return builder.routes()
         .route("order-service", r -> r
           .path("/api/v1/orders/**")
           .filters(f -> f
             .stripPrefix(2)
             .requestRateLimiter(c -> c
               .setRateLimiter(redisRateLimiter())
               .setKeyResolver(ipAddressKeyResolver())
             )
             .retry(c -> c
               .setRetries(3)
               .setMethods(HttpMethod.GET, HttpMethod.POST)
             )
           )
           .uri("http://order-service:8080")
         )
         .build();
     }

     @Bean
     public RedisRateLimiter redisRateLimiter() {
       return new RedisRateLimiter(10, 20); // 10 req/s, 20 burst
     }
   }
   ```

4. **Kong Configuration**
   ```bash
   # Add upstream (service target)
   curl -X POST http://localhost:8001/upstreams \
     -d "name=order-service"

   # Add service
   curl -X POST http://localhost:8001/services \
     -d "name=order" \
     -d "url=http://order-service:8080"

   # Add route
   curl -X POST http://localhost:8001/services/order/routes \
     -d "paths[]=/api/v1/orders" \
     -d "strip_path=true"

   # Add authentication plugin
   curl -X POST http://localhost:8001/services/order/plugins \
     -d "name=oauth2" \
     -d "config.scopes=orders:read,orders:write"

   # Add rate limiting plugin
   curl -X POST http://localhost:8001/services/order/plugins \
     -d "name=rate-limiting" \
     -d "config.minute=100" \
     -d "config.hour=1000"
   ```

5. **Authentication & Authorization**
   ```java
   @Configuration
   @EnableWebFluxSecurity
   public class SecurityConfig {

     @Bean
     public SecurityWebFilterChain springSecurityFilterChain(
         ServerHttpSecurity http) {
       return http
         .authorizeExchange(authz -> authz
           .pathMatchers("/api/public/**").permitAll()
           .pathMatchers("/api/v1/accounts/**").hasScope("accounts:read")
           .pathMatchers("/api/v1/payments/**").hasScope("payments:write")
           .anyExchange().authenticated()
         )
         .oauth2ResourceServer(oauth2 -> oauth2
           .jwt(jwt -> jwt.decoder(jwtDecoder()))
         )
         .csrf().disable()
         .build();
     }

     @Bean
     public JwtDecoder jwtDecoder() {
       return NimbusJwtDecoder.withJwkSetUri(
         "https://auth-server.techcombank.com/.well-known/jwks.json"
       ).build();
     }
   }
   ```

6. **Logging & Tracing**
   ```java
   @Component
   public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {

     private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

     @Override
     public GatewayFilter apply(Config config) {
       return (exchange, chain) -> {
         ServerHttpRequest request = exchange.getRequest();
         String traceId = UUID.randomUUID().toString();

         log.info("Request: {} {} | TraceId: {}",
           request.getMethod(), request.getPath(), traceId);

         return chain.filter(exchange)
           .doFinally(signal -> {
             ServerHttpResponse response = exchange.getResponse();
             log.info("Response: {} | Status: {} | TraceId: {}",
               request.getPath(), response.getStatusCode(), traceId);
           });
       };
     }

     public static class Config {
     }
   }
   ```

## API Gateway Responsibilities

| Concern | Implementation |
|---------|---|
| **Routing** | Based on path, hostname, method |
| **Authentication** | OAuth2/OIDC token validation |
| **Authorization** | Scopes, roles check |
| **Rate Limiting** | Token bucket, sliding window |
| **Circuit Breaking** | Fail fast on service outage |
| **Caching** | Cache responses (GET, 200s) |
| **Logging** | Structured logs with trace IDs |
| **Request/Response Transformation** | Add headers, rewrite paths |
| **Protocol Conversion** | REST to gRPC, WebSocket upgrade |
| **Monitoring** | Metrics (latency, errors, throughput) |

## When to Use

- Multiple microservices serving the same client
- Need centralized authentication/authorization
- Rate limiting per customer/API key
- Request/response transformation
- Service discovery (gateway discovers services)
- API versioning

## When NOT to Use

- Single service (unnecessary complexity)
- Service-to-service (use service mesh instead)
- Extreme performance requirements (adds latency)

## Best Practices

1. **Keep gateway thin**: Business logic belongs in services
2. **Cache responses**: Reduce backend load
3. **Set timeout limits**: Prevent hanging connections
4. **Monitor gateway**: Track latency, errors, throughput
5. **Version API contracts**: Maintain backward compatibility
6. **Use correlation IDs**: Trace requests across services

## References

- [Kong API Gateway](https://konghq.com/)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [AWS API Gateway](https://aws.amazon.com/api-gateway/)
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)

---

**Key Takeaway**: Implement a single API Gateway for routing, auth, rate limiting, and logging. Keeps services simple; centralizes cross-cutting concerns.
