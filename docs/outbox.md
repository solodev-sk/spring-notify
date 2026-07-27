# Transactional outbox

The `notify-spring-boot-outbox` module makes a send **durable and transactionally consistent**:
you persist the notification in the same database transaction as your business change, and a
background relay delivers it afterwards.

```xml
<dependency>
    <groupId>sk.solodev</groupId>
    <artifactId>notify-spring-boot-outbox</artifactId>
</dependency>
```

## Why

`notifier.notify(request)` calls the provider inline. That leaves two gaps:

- **Lost notification** — the business transaction commits, then the process dies before the
  provider call completes. The customer never hears about their order.
- **Phantom notification** — you send first, then the surrounding transaction rolls back. The
  customer is told about an order that does not exist.

The outbox closes both. If the transaction commits, the notification *will* be delivered; if it
rolls back, nothing is sent.

## Use

Inject `OutboxNotifier` instead of `Notifier` and call `enqueue` inside a transactional method:

```java
@Service
class OrderService {

    private final OrderRepository orders;
    private final OutboxNotifier outbox;

    OrderService(OrderRepository orders, OutboxNotifier outbox) {
        this.orders = orders;
        this.outbox = outbox;
    }

    @Transactional
    public void placeOrder(Order order) {
        orders.save(order);
        // Written in the same transaction as the save: roll back and nothing is sent.
        outbox.enqueue(EmailRequest.builder()
                .to(order.customerEmail())
                .from("shop@example.com")
                .subject("Order received")
                .body("Thanks for your order!")
                .build());
    }
}
```

`enqueue` returns the **outbox entry id**, not a provider message id — at that point nothing has
been sent yet. Use the id to correlate with the `NotificationSent` / `NotificationFailed` event
published when the relay actually delivers.

Which bean you inject *is* the durability choice: `Notifier` sends inline, `OutboxNotifier`
persists for durable delivery. Both can be used in the same application.

## The database table

The outbox needs one table. The module ships the canonical DDL as
`schema-notification-outbox.sql` (on the classpath), but **does not run it** — your application
owns its schema:

- **Production** — copy it into your Flyway/Liquibase migrations.
- **Dev/test** — point `spring.sql.init.schema-locations` at
  `classpath:schema-notification-outbox.sql`.

The DDL is written for PostgreSQL. For MySQL 8+, use `CHAR(36)` for `id` and `DATETIME(6)` for the
timestamps. The relay's claim query uses `FOR UPDATE SKIP LOCKED`, which requires **PostgreSQL or
MySQL 8.0+**.

Override the table name with `spring.notify.outbox.table-name` if `notification_outbox` clashes.

## How delivery works

```
@Transactional method
  orders.save(order)
  outbox.enqueue(request) ──► INSERT ... status=PENDING     (same transaction)
                     commit ──► row is durable
                                    │
OutboxRelay (every poll-interval, on every instance)
  SELECT ... WHERE status='PENDING' AND next_attempt_at <= now
      FOR UPDATE SKIP LOCKED LIMIT batch-size
  per row: deserialize ──► notifier.notify(request) ──► status=SENT, message_id
           on failure  ──► attempts++, next_attempt_at = now + backoff   (or FAILED)
```

The relay delivers through the **normal `Notifier` pipeline**, so your interceptors, events, and
observations all apply at real delivery time — the outbox composes on top of them rather than
bypassing them.

**Multi-instance safe.** `SKIP LOCKED` means concurrent relays on different instances claim
disjoint rows, so you can run the outbox on every instance without double-sending. No leader
election required.

## Retries and failure

A failed delivery increments `attempts` and schedules the next try with exponential backoff
(doubling from `initial-backoff`, capped at `max-backoff`). Once `attempts` reaches
`max-attempts`, the entry becomes `FAILED` and is no longer retried; `last_error` holds the final
message.

A payload that cannot be deserialized at all — the request class was renamed or removed, or the
stored JSON no longer matches it — is marked `FAILED` immediately rather than retried, since it can
never succeed. This keeps one bad row from blocking every other entry in the batch.

Retries are durable and cross-poll: `attempts` and `next_attempt_at` live in the table, so a pending
retry survives restarts and redeploys. If you also write a retrying
[interceptor](interceptors.md), note that its attempts sit *inside* one relay attempt, so the two
counts multiply.

**Delivery is at-least-once.** If the process dies after the provider accepts the message but
before the status update commits, the relay will deliver that entry again on a later poll. Design
consumers accordingly, or make the sends idempotent at the provider level.

## Configuration

| Key                                    | Default               | Meaning                                          |
|----------------------------------------|-----------------------|--------------------------------------------------|
| `spring.notify.outbox.poll-interval`   | `PT5S`                | how often the relay polls                        |
| `spring.notify.outbox.batch-size`      | `100`                 | rows claimed per poll                            |
| `spring.notify.outbox.max-attempts`    | `5`                   | attempts before an entry is `FAILED`             |
| `spring.notify.outbox.initial-backoff` | `PT10S`               | delay before the first retry                     |
| `spring.notify.outbox.max-backoff`     | `PT10M`               | cap on the exponential backoff                   |
| `spring.notify.outbox.table-name`      | `notification_outbox` | outbox table name                                |

```yaml
spring:
  notify:
    outbox:
      poll-interval: 2s
      max-attempts: 8
```

Adding the module is the opt-in — every key above is optional. The outbox auto-configures as soon as
a `DataSource` bean is present.

## Supplying your own store

Persistence sits behind the `OutboxStore` SPI (`insert`, `claimBatch`, `markSent`, `markForRetry`,
`markFailed`). The JDBC implementation is registered `@ConditionalOnMissingBean`, so defining your
own `OutboxStore` bean replaces it — useful for a non-relational store or a different claim
strategy.