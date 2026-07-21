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
- [Observability & events](observability-and-events.md) — Micrometer observation, lifecycle events, interceptor ordering.
- [Testing](testing.md) — asserting notifications with `RecordingNotifier`.

> Status: `1.0.0-SNAPSHOT` · Java 25 · Spring Boot 4.1. Not yet on Maven Central — build locally
> with `./mvnw install`.

For a one-page overview, see the [README](../README.md).
