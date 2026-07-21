# spring-notify

**Channel-agnostic notification delivery for Spring Boot.** Send SMS, push, email, and chat
messages through one small API — your code talks to a *channel*, never to a *provider*.
Swap Twilio for another SMS gateway, or FCM for APNs, by changing a dependency, not your code.

```java
notifier.notify(SmsRequest.builder()
        .to("+421900123456")
        .from("+421900999888")
        .message("Your order has shipped")
        .build());
```

> Status: `1.0.0-SNAPSHOT` · Java 25 · Spring Boot 4.1 · no runtime dependencies beyond
> Spring and the provider SDK you choose.

📖 **Full documentation lives in [`docs/`](docs/index.md).** This page is the overview.

---

## Why

Most apps end up with provider SDKs (`TwilioRestClient`, `FirebaseMessaging`, a `JavaMailSender`,
a Slack client) threaded directly through business code. Switching providers, adding a channel,
or testing delivery then means touching every call site.

spring-notify puts a thin, provider-neutral layer in front:

- **One entry point** — inject `Notifier`, call `notify(request)`.
- **Typed, immutable requests** — `SmsRequest`, `PushRequest`, `EmailRequest`, `ChatRequest`
  (fluent builders; jspecify-annotated, validated at build time).
- **Providers are plug-ins** — each is a separate starter that contributes one `*Sender` bean.
  Auto-configuration wires it in when it's configured.
- **Routing by type** — the request's concrete type selects the channel; no `if/switch` in your code.

---

## Quick start

**1. Build it locally** (not yet on Maven Central):

```bash
git clone <repo> && cd spring-notify && ./mvnw install
```

**2. Add a provider starter** — it pulls in the channel and pipeline transitively:

```xml
<dependency>
    <groupId>sk.solodev</groupId>
    <artifactId>notify-spring-boot-starter-sms-twilio</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**3. Configure it** (`application.yml`) — keys autocomplete in your IDE:

```yaml
spring:
  notify:
    sms:
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
```

**4. Inject `Notifier` and send:**

```java
String messageId = notifier.notify(SmsRequest.builder()
        .to(order.phone())
        .from("+421900999888")
        .message("Your order has shipped")
        .build());
```

`notify(...)` returns the provider message id, or throws `NotificationDeliveryException` (carrying
the failed request) on failure. Use `notifyAsync(...)` to send off the calling thread.

→ [Getting started](docs/getting-started.md) covers the BOM, async, and the full walkthrough.

---

## Channels & providers

| Channel | Request | Provider starters |
|---------|---------|-------------------|
| SMS   | `SmsRequest`   | `…-sms-twilio`, `…-sms-vonage` |
| Push  | `PushRequest`  | `…-push-fcm`, `…-push-apns` |
| Email | `EmailRequest` | `…-email-smtp`, `…-email-sendgrid` |
| Chat  | `ChatRequest`  | `…-chat-slack` |

**Providers are interchangeable.** Each channel takes the same request whichever backend you
install — you pick it with a dependency, not a code change. Your app code never mentions Twilio,
Vonage, FCM, APNs, or SendGrid. Not using a bundled provider? Depend on the channel module and
supply your own `*Sender` bean.

→ [Providers](docs/providers.md) has configuration for every backend · [Channels](docs/channels.md)
documents the request fields.

---

## What else it does

- **Asynchronous sending** — `notifyAsync` runs the pipeline off-thread on Boot's task executor
  (virtual-thread and context-propagation aware). → [Concepts](docs/concepts.md#asynchronous-sending)
- **Observability** — a Micrometer timer, span, and logs per send when an `ObservationRegistry` is
  present; delivery-scoped by design. → [Observability & events](docs/observability-and-events.md)
- **Lifecycle events** — `NotificationSent` / `NotificationFailed` published per send for audit,
  dead-lettering, or retry. → [Observability & events](docs/observability-and-events.md#events)
- **Interceptors** — wrap every send (or one channel) for logging, rate limiting, retry, kill
  switches. → [Concepts](docs/concepts.md#the-pipeline)
- **Test double** — `RecordingNotifier` asserts sends without a real provider.
  → [Testing](docs/testing.md)

---

## How it fits together

```
your code → Notifier.notify(request)
              → NotificationInterceptor chain      (ordered, optional)
              → AdapterResolver                    (picks the adapter by request type)
              → ChannelAdapter.deliver(request)    (wraps failures, returns message id)
                  → NotificationSender.send(request)   (the provider call: Twilio/FCM/SMTP/Slack)
```

- **`notify-core`** — the pipeline and contracts. No provider dependencies.
- **`notify-spring-boot-<channel>`** — a channel: its request type, `*Sender` SPI, and a generic adapter.
- **`notify-spring-boot-starter-<channel>-<provider>`** — one provider implementation you install.

You depend on a provider starter; everything below it comes transitively.

→ [Concepts](docs/concepts.md) explains routing, the error model, and immutability.

---

## Requirements

- Java 25
- Spring Boot 4.1+

## What's next

- More providers per channel — SMS (AWS SNS), email (SES), chat (Discord).
- Publishing to Maven Central.

Contributions and provider requests welcome.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
