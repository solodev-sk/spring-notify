# Observability & events

Both features are cross-cutting interceptors contributed automatically. They sit at opposite ends
of the chain from user interceptors, and both reflect the **actual delivery** rather than the
logical `notify()` call.

## Observability

When an `ObservationRegistry` bean is present, every send is wrapped in a Micrometer `Observation`
— one **timer**, one **tracing span**, and structured **logs**, tagged with the channel
(`notify.channel = sms | push | email | chat`). Without a registry bean nothing is registered and
sends are simply unobserved.

The registry bean is contributed by Boot's `spring-boot-micrometer-observation` module. You get it
by depending on that module directly, or transitively — `spring-boot-starter-actuator` is the usual
way. (In Boot 4 this moved out of `spring-boot-actuator-autoconfigure` into its own module, so
Actuator is no longer where the observation auto-configuration lives — just a convenient way to pull
it in.)

### Delivery-scoped by design

The observation interceptor runs **innermost**, so `spring.notify.send` times the provider call
itself — not your interceptors. A rate-limiter's wait or a retry wrapper's backoff falls *outside*
the span, so the timer reflects provider latency rather than the caller's total wait (which the
enclosing request or scheduled-task span already captures). Under a retry interceptor, each attempt
is its own span.

To measure the caller's total wait, read the enclosing span, not this one.

### Customising

Declare your own `NotificationObservationConvention` bean to change the observation name or tags.

## Events

Every send publishes a Spring application event — `NotificationSent` (with the provider message id)
on success, `NotificationFailed` (with the cause) on failure. Listen with `@EventListener` for
audit trails, dead-lettering, or your own retry — no interceptor boilerplate:

```java
@Component
class DeliveryAudit {

    @EventListener
    void onSent(NotificationSent event) {
        log.info("sent {} via {}", event.messageId(), event.request().getClass().getSimpleName());
    }

    @EventListener
    void onFailed(NotificationFailed event) {
        deadLetterStore.save(event.request());
    }
}
```

Both implement the sealed `NotificationEvent`, so one listener can `switch` over them. Enabled by
default; set `spring.notify.events.enabled=false` to turn publication off.

The event interceptor runs just outside the observation interceptor — near-innermost — so events
also reflect the actual delivery (a short-circuit that never reaches the provider publishes nothing;
a retry wrapper sees one `NotificationFailed` per real attempt), while the delivery span still
excludes `@EventListener` execution time.

## Interceptor ordering, end to end

From outermost to innermost:

```
[ your interceptors ]         (@Order / Ordered)
  [ event publishing ]        (LOWEST_PRECEDENCE - 1)
    [ observation ]           (LOWEST_PRECEDENCE, innermost)
      resolve + provider send
```
