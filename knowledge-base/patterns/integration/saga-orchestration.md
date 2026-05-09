# SAGA Orchestration Pattern

Status: Approved | Last Reviewed: 2026-02-10 | Owner: @ea-board

## Problem Statement

Microservices struggle with distributed transactions across multiple databases:
- Cannot use traditional 2-phase commit (blocking, unreliable in distributed systems)
- ACID transactions span multiple services and databases
- Long-running business transactions involve approval workflows, payment processing, inventory reservation
- Failure recovery is complex: which service failed? How to compensate?

## Solution

Use a central orchestrator to coordinate a sequence of local transactions. Each step executes a compensating transaction on failure.

```
                      SAGA ORCHESTRATOR
                      ================
                           |
                           |
         |------- Command -------|
         |                       |
    Order Service          Payment Service
    ============           ==============
    1. ReserveOrder       1. ProcessPayment
       (local tx)            (local tx)
    2. OrderReserved      2. PaymentProcessed
       (emit event)          (emit event)
         |                       |
         +----------- Orchestrator listens to events ----------+
                              |
                              |
                        Inventory Service
                        ================
                        1. ReserveInventory
                           (local tx)
                        2. InventoryReserved
                           (emit event)
```

## Implementation Guidelines

1. **Define SAGA Steps**
   - Each step is a local transaction in a service
   - Each step has a success path and compensation (undo)
   - Order SAGA:
     - Step 1 (Order Service): Create order, emit `OrderCreated`
     - Step 2 (Payment Service): Process payment, emit `PaymentProcessed`
     - Step 3 (Inventory Service): Reserve items, emit `InventoryReserved`
     - Step 4 (Notification Service): Send confirmation

2. **Orchestrator Implementation**
   - Centralized SAGA orchestrator tracks state
   - Options: Apache Temporal, Camunda, custom Spring service
   - Maintains SAGA instance state: running, completed, failed
   - Triggers next step based on previous success/failure

3. **Compensation Logic**
   ```java
   @Service
   @Transactional
   public class OrderSagaOrchestrator {

     private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

     @Autowired
     private OrderService orderService;
     @Autowired
     private PaymentService paymentService;
     @Autowired
     private InventoryService inventoryService;

     public void executeOrderSaga(Order order) {
       String sagaId = UUID.randomUUID().toString();
       SagaInstance saga = new SagaInstance(sagaId, "ORDER_SAGA", order.getId());

       try {
         // Step 1: Create Order
         saga.step(1, "CREATE_ORDER");
         Order createdOrder = orderService.createOrder(order);
         saga.markStepSuccess(1);

         // Step 2: Process Payment
         saga.step(2, "PROCESS_PAYMENT");
         PaymentResult payment = paymentService.processPayment(
           createdOrder.getCustomerId(),
           createdOrder.getAmount()
         );
         saga.markStepSuccess(2);

         // Step 3: Reserve Inventory
         saga.step(3, "RESERVE_INVENTORY");
         inventoryService.reserveItems(createdOrder.getItems());
         saga.markStepSuccess(3);

         // All steps succeeded
         saga.markCompleted();
         log.info("Order SAGA completed: orderId={}", createdOrder.getId());

       } catch (PaymentFailedException e) {
         log.error("Payment failed, rolling back order: {}", sagaId);
         // Compensation: reverse steps
         compensateOrderCreation(order);
         saga.markFailed(2, e.getMessage());
         throw new OrderSagaFailedException(sagaId, 2);

       } catch (InventoryUnavailableException e) {
         log.error("Inventory unavailable, reversing payment and order: {}", sagaId);
         // Compensation: reverse steps 2 and 1
         compensatePayment(order);
         compensateOrderCreation(order);
         saga.markFailed(3, e.getMessage());
         throw new OrderSagaFailedException(sagaId, 3);
       }
     }

     private void compensateOrderCreation(Order order) {
       orderService.cancelOrder(order.getId());
     }

     private void compensatePayment(Order order) {
       paymentService.refund(order.getId());
     }
   }
   ```

4. **Temporal Implementation** (Recommended)
   ```java
   public interface OrderSaga {
     @WorkflowMethod
     void executeOrderSaga(Order order);
   }

   public class OrderSagaImpl implements OrderSaga {
     private final OrderService orderService = Workflow.newActivityStub(
       OrderService.class,
       new ActivityOptions.Builder()
         .setStartToCloseTimeout(Duration.ofMinutes(5))
         .setRetryOptions(new RetryOptions.Builder()
           .setInitialInterval(Duration.ofSeconds(1))
           .setMaximumInterval(Duration.ofSeconds(60))
           .setBackoffCoefficient(2)
           .setMaximumAttempts(3)
           .build())
         .build()
     );

     @Override
     public void executeOrderSaga(Order order) {
       try {
         // Step 1: Create order
         Order createdOrder = orderService.createOrder(order);

         // Step 2: Process payment (with retry)
         PaymentResult payment = orderService.processPayment(
           createdOrder.getCustomerId(),
           createdOrder.getAmount()
         );

         // Step 3: Reserve inventory
         orderService.reserveInventory(createdOrder.getItems());

         // Success
       } catch (ActivityFailureException e) {
         // Temporal automatically handles compensation
         if (createdOrder != null) {
           orderService.compensateOrder(createdOrder.getId());
         }
         throw e;
       }
     }
   }
   ```

5. **Failure Handling**
   - Idempotency: Ensure compensation is idempotent (can run multiple times safely)
   - Retry: Transient failures (timeout, network) → automatic retry
   - Circuit breaker: Persistent failures (service down) → fail fast
   - Dead-letter queue: Unrecoverable failures → manual intervention

6. **Monitoring SAGA Execution**
   - Log each step: start, success, failure, compensation
   - Metrics: saga duration, success rate, compensation frequency
   - Alerts: if SAGA fails repeatedly, notify ops team

## Happy Path vs Compensation

```
HAPPY PATH:
  Order Service: ReserveOrder() → Success
       ↓
  Payment Service: ProcessPayment() → Success
       ↓
  Inventory Service: ReserveInventory() → Success
       ↓
  SAGA COMPLETED

FAILURE PATH (Payment fails):
  Order Service: ReserveOrder() → Success
       ↓
  Payment Service: ProcessPayment() → FAILURE
       ↓
  Inventory Service: SKIPPED
       ↓
  Compensation Step 1: CancelOrder()
       ↓
  SAGA ROLLED BACK
```

## When to Use

- Multi-step business processes
- Approval workflows (order → manager approval → fulfillment)
- Long-running transactions (minutes to hours)
- Cross-service coordination
- Financial transactions requiring rollback capability

## When NOT to Use

- Simple service calls (use direct API calls)
- Real-time constraints (SAGA may take seconds)
- Compensation is impossible (e.g., physical goods already shipped)

## Tools

| Tool | Use Case |
|------|----------|
| **Apache Temporal** | Complex workflows, strong semantics, recommended |
| **Camunda** | BPMN workflows, human approvals |
| **Axon Framework** | Event sourcing with SAGA support |
| **Custom Spring Service** | Simple SAGAs, full control |

## References

- [Apache Temporal](https://temporal.io/)
- [Camunda](https://camunda.com/)
- [SAGA Pattern](https://microservices.io/patterns/data/saga.html)
- [Axon Framework](https://axoniq.io/)

---

**Key Takeaway**: Use SAGA Orchestration for distributed transactions. Each step is a local transaction; failures trigger compensation. Use Temporal for production systems.
