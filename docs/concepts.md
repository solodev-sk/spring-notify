# Concepts

## The channel/provider split

spring-notify separates *what* you send from *how* it's delivered.

- A **channel** is a kind of notification — SMS, push, email, chat. Each defines a request type
  (`SmsRequest`, `PushRequest`, …) and a sender SPI (`SmsSender`, …). Channels live in the
  `notify-spring-boot-<channel>` modules and carry no provider dependencies.
- A **provider** is a concrete backend for a channel — Twilio, Vonage, FCM, APNs, SMTP, SendGrid,
  Slack. Each is a thin starter (`notify-spring-boot-starter-<channel>-<provider>`) that implements
  the channel's sender SPI and auto-configures itself from properties.

Your code depends on the *provider starter* but only ever references the *channel* request type. To
switch backends you change the dependency; the code that builds and sends requests is untouched.

## The pipeline

```
your code → Notifier.notify(request)
              → NotificationInterceptor chain      (ordered, optional)
              → AdapterResolver                    (picks the adapter by request type)
              → ChannelAdapter.deliver(request)    (wraps failures, returns message id)
                  → NotificationSender.send(request)   (the provider call)
```

- **`Notifier`** is the single entry point you inject. `DefaultNotifier` runs the request through
  the interceptor chain, then hands it to the resolver.
- **`NotificationInterceptor`** wraps every send — for cross-cutting behaviour (logging, rate
  limiting, retry, a feature-flag kill-switch). Global by default; extend `ChannelInterceptor<T>`
  to scope one to a single channel. Interceptors are ordered with `@Order`/`Ordered` and
  `DefaultNotifier` sorts them itself.
- **`AdapterResolver`** selects the one adapter whose request type matches. Exactly one provider
  must be installed per channel; zero or more than one is a wiring error (`IllegalStateException`).
- **`ChannelAdapter` / `SenderChannelAdapter`** invoke the provider's `NotificationSender` and
  normalise any failure to a `NotificationDeliveryException` carrying the failed request.

## Routing by type

There is no `if`/`switch` on channel in your code. The request's concrete type *is* the routing
key: an `SmsRequest` resolves to the SMS adapter, an `EmailRequest` to the email adapter. Install
several providers across channels and they compose — each request goes to its matching channel.

## Requests are immutable and validated at build time

Request types are records built with fluent builders. `build()` rejects missing or blank required
fields (`Assert.hasText`), so a malformed request fails locally at construction rather than after a
network round-trip. Nullness is a compile-time contract (jspecify). Provider-specific extras that
aren't portable ride in an `attributes` map the sender reads by key.

## Asynchronous sending

`notifyAsync(request)` runs the whole pipeline off the calling thread and returns a
`CompletableFuture<String>`. It uses Spring Boot's `applicationTaskExecutor`, so:

- `spring.threads.virtual.enabled=true` makes every async send use a virtual thread.
- `spring.task.execution.propagate-context=true` carries the caller's tracing span, MDC, and
  security context onto the worker thread — the same mechanism as Spring's `@Async`.

## Error model

- **`NotificationDeliveryException`** (unchecked) — the provider failed to deliver. Carries
  `request()` and the underlying cause. Catch it to retry, log, or fall back.
- **`IllegalStateException`** — a wiring problem (no provider, or more than one, for a request
  type). A configuration bug to fix, not a runtime condition to handle.
- **`IllegalArgumentException`** — a request built with a missing/blank required field.

---

Next: [Channels](channels.md) — the request types and their fields.
