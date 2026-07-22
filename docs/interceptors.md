# Interceptors

An interceptor wraps **every** send, running around the provider call. It's the framework's main
extension point — where you add cross-cutting behaviour the library deliberately doesn't impose:
logging, rate limiting, retry, fallback, a feature-flag kill switch, metrics.

Register a `NotificationInterceptor` bean and it joins the chain automatically. Interceptors are
ordered by `@Order`/`Ordered` (lowest value runs outermost) and `DefaultNotifier` sorts them for
you.

## A global interceptor

Implement `NotificationInterceptor` and call `chain.proceed(request)` to continue the pipeline. What
you do before and after is up to you:

```java
@Component
class LoggingInterceptor implements NotificationInterceptor {

    public String intercept(NotificationRequest request, Chain chain) {
        long start = System.nanoTime();
        String messageId = chain.proceed(request);
        log.info("sent {} in {}ms", request.getClass().getSimpleName(), (System.nanoTime() - start) / 1_000_000);
        return messageId;
    }
}
```

The return value is the provider message id. `chain.proceed(...)` throws
`NotificationDeliveryException` if delivery fails — catch it to retry, fall back, or swallow.

## Short-circuiting

An interceptor doesn't have to call the chain. Return a message id without proceeding to stop the
send — useful for a kill switch, deduplication, or quiet hours:

```java
@Component
class KillSwitchInterceptor implements NotificationInterceptor {

    public String intercept(NotificationRequest request, Chain chain) {
        if (notificationsDisabled()) {
            return "suppressed";        // never reaches the provider
        }
        return chain.proceed(request);
    }
}
```

## Scoping to one channel

To run logic for a single channel only, extend `ChannelInterceptor<T>`. It runs
`interceptForChannel` for requests of type `T` and passes everything else straight through — no
`instanceof` guard:

```java
@Component
class SmsRateLimiter extends ChannelInterceptor<SmsRequest> {

    SmsRateLimiter() {
        super(SmsRequest.class);
    }

    protected String interceptForChannel(SmsRequest request, Chain chain) {
        rateLimiter.acquire();          // runs only for SMS
        return chain.proceed(request);
    }
}
```

## Ordering

Multiple interceptors are ordered with `@Order` (or by implementing `Ordered`); the lowest value
runs outermost, wrapping the others. The built-in event and observation interceptors sit innermost
(closest to the provider), so your interceptors wrap them — see
[Observability & events](observability-and-events.md#interceptor-ordering-end-to-end) for the full
picture.

## Retry, rate limiting, fallback

These are yours to add here, not built in. Wrap `chain.proceed(...)` with Spring's `RetryTemplate`,
Resilience4j, a token bucket, or your own logic. The library deliberately imposes no retry policy —
whether a resend is safe (idempotency) is your call, not the framework's.

```java
@Component
class RetryingInterceptor implements NotificationInterceptor {

    private final RetryTemplate retry = /* your policy */;

    public String intercept(NotificationRequest request, Chain chain) {
        return retry.execute(ctx -> chain.proceed(request));
    }
}
```

---

Next: [Observability & events](observability-and-events.md) — the built-in interceptors.
