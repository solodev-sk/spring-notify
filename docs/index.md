# spring-notify documentation

Channel-agnostic notification delivery for Spring Boot. Your code talks to a *channel*
(SMS, push, email, chat) through one small API; a *provider* starter backs that channel with a
concrete service (Twilio, FCM, SMTP, …). Swap the provider by changing a dependency, not your code.

```java
notifier.notify(SmsRequest.builder()
        .to("+421900123456")
        .from("+421900999888")
        .message("Your order has shipped")
        .build());
```

## Contents

- [Getting started](getting-started.md) — install with the BOM, send your first notification.
- [Concepts](concepts.md) — the pipeline, the channel/provider split, how routing works.
- [Channels](channels.md) — the four request types and their fields.
- [Providers](providers.md) — the provider matrix and per-provider configuration.
- [Interceptors](interceptors.md) — wrap every send for logging, rate limiting, retry, kill switches.
- [Observability & events](observability-and-events.md) — Micrometer observation, lifecycle events, interceptor ordering.
- [Transactional outbox](outbox.md) — durable sends that commit with your business transaction.
- [Testing](testing.md) — asserting notifications with `RecordingNotifier`.

> Latest release: `1.0.0` (on Maven Central) · Java 25 · Spring Boot 4.1.

For a one-page overview, see the [README](../README.md).
