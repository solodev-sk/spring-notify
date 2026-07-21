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

> Status: `0.1.0-SNAPSHOT` · Java 25 · Spring Boot 4.1 · no runtime dependencies beyond
> Spring and the provider SDK you choose.

---

## Building

Not yet on Maven Central — build it into your local `~/.m2` first:

```bash
git clone <repo> && cd spring-notify
./mvnw install
```

Then depend on the starter(s) you need, as shown below.

---

## Why

Most apps end up with provider SDKs (`TwilioRestClient`, `FirebaseMessaging`, a `JavaMailSender`,
a Slack client) threaded directly through business code. Switching providers, adding a channel,
or testing delivery then means touching every call site.

spring-notify puts a thin, provider-neutral layer in front:

- **One entry point** — inject `Notifier`, call `notify(request)`.
- **Typed, immutable requests** — `SmsRequest`, `PushRequest`, `EmailRequest`, `ChatRequest`
  (built with fluent builders; jspecify-annotated, non-null by default).
- **Providers are plug-ins** — each is a separate starter that contributes one `*Sender` bean.
  Auto-configuration wires it in when it's configured.
- **Routing by type** — the request's concrete type selects the channel; no `if/switch` in your code.

---

## Quick start

Say you want to send SMS via Twilio.

**1. Add the provider starter** (it pulls in the SMS channel + the pipeline transitively):

```xml
<dependency>
    <groupId>sk.solodev</groupId>
    <artifactId>notify-spring-boot-starter-sms-twilio</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

**2. Configure the provider** (`application.yml`):

```yaml
spring:
  notify:
    sms:
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
```

**3. Inject `Notifier` and send:**

```java
@Service
class OrderService {

    private final Notifier notifier;

    OrderService(Notifier notifier) {
        this.notifier = notifier;
    }

    void onShipped(Order order) {
        String messageId = notifier.notify(SmsRequest.builder()
                .to(order.phone())
                .from("+421900999888")
                .message("Your order has shipped")
                .build());
    }
}
```

`notify(...)` returns the provider message id, or throws `NotificationDeliveryException`
(carrying the failed request) if delivery fails.

### Sending asynchronously

Delivery is network I/O — an SMTP round-trip or an HTTP call to a provider. To avoid blocking
the caller, use `notifyAsync`, which runs the whole pipeline (interceptors, routing, the provider
call) off the calling thread and hands back a `CompletableFuture<String>`:

```java
notifier.notifyAsync(SmsRequest.builder()
                .to(order.phone())
                .from("+421900999888")
                .message("Your order has shipped")
                .build())
        .thenAccept(messageId -> log.info("sent {}", messageId))
        .exceptionally(ex -> { log.warn("send failed", ex); return null; });
```

It runs on Spring Boot's `applicationTaskExecutor`, so setting `spring.threads.virtual.enabled=true`
makes every async send use a virtual thread — no extra configuration. The future completes
exceptionally with the same `NotificationDeliveryException` that `notify(...)` would throw.

---

## Channels & providers

Each **channel** defines a request type and an SPI; each **provider** is a starter that
implements the SPI for one backend.

| Channel | Request | Provider starter | Config prefix |
|---------|---------|------------------|---------------|
| SMS   | `SmsRequest`   | `notify-spring-boot-starter-sms-twilio`   | `spring.notify.sms.twilio`   |
| SMS   | `SmsRequest`   | `notify-spring-boot-starter-sms-vonage`   | `spring.notify.sms.vonage`   |
| Push  | `PushRequest`  | `notify-spring-boot-starter-push-fcm`     | `spring.notify.push.fcm`     |
| Push  | `PushRequest`  | `notify-spring-boot-starter-push-apns`    | `spring.notify.push.apns`    |
| Email | `EmailRequest` | `notify-spring-boot-starter-email-smtp`     | `spring.notify.email.smtp`     |
| Email | `EmailRequest` | `notify-spring-boot-starter-email-sendgrid` | `spring.notify.email.sendgrid` |
| Chat  | `ChatRequest`  | `notify-spring-boot-starter-chat-slack`   | `spring.notify.chat.slack`   |

Add as many as you need — they compose. `notifier.notify(...)` routes each request to the
matching channel by its type.

**Providers are interchangeable.** Every channel with more than one provider — SMS (Twilio,
Vonage), push (Firebase for Android/web, APNs for iOS), email (SMTP, SendGrid) — takes the same
request; you pick the backend with a dependency, not a code change. Your app code never mentions
Twilio, Vonage, FCM, APNs, or SendGrid.

### SMS — Twilio or Vonage

The same `SmsRequest` works with either provider. Add the starter for the backend you use.

**Twilio** — `notify-spring-boot-starter-sms-twilio`:

```yaml
spring:
  notify:
    sms:
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
```

**Vonage** — `notify-spring-boot-starter-sms-vonage`:

```yaml
spring:
  notify:
    sms:
      vonage:
        api-key: ${VONAGE_API_KEY}
        api-secret: ${VONAGE_API_SECRET}
```

```java
notifier.notify(SmsRequest.builder()
        .to("+421900123456")
        .from("+421900999888")
        .message("Hello from spring-notify")
        .build());
```

### Push — Firebase Cloud Messaging (Android/web) or APNs (iOS)

The same `PushRequest` works with either provider. Add the starter for the backend you target.

**Firebase Cloud Messaging** — `notify-spring-boot-starter-push-fcm`:

```yaml
spring:
  notify:
    push:
      fcm:
        service-account: ${FCM_SERVICE_ACCOUNT_JSON}   # the service-account JSON, inline
```

**APNs** (Apple Push Notification service, token auth) — `notify-spring-boot-starter-push-apns`:

```yaml
spring:
  notify:
    push:
      apns:
        signing-key: ${APNS_SIGNING_KEY}   # the .p8 key contents
        key-id: ${APNS_KEY_ID}
        team-id: ${APNS_TEAM_ID}
        topic: com.example.app             # your app's bundle id
        production: true                   # false = sandbox
```

Sending is identical regardless of provider:

```java
notifier.notify(PushRequest.builder()
        .to(deviceToken)
        .title("Order shipped")
        .body("Your order is on its way")
        .attribute("orderId", "12345")   // FCM data / APNs custom payload
        .build());
```

### Email — SMTP or SendGrid

The same `EmailRequest` works with either provider. Add the starter for the backend you use.

**SMTP** — `notify-spring-boot-starter-email-smtp`:

```yaml
spring:
  notify:
    email:
      smtp:
        host: smtp.example.com
        port: 587
        username: ${SMTP_USERNAME}
        password: ${SMTP_PASSWORD}
```

**SendGrid** — `notify-spring-boot-starter-email-sendgrid`:

```yaml
spring:
  notify:
    email:
      sendgrid:
        api-key: ${SENDGRID_API_KEY}
```

```java
notifier.notify(EmailRequest.builder()
        .to("customer@example.com")
        .from("shop@example.com")
        .subject("Your order shipped")
        .body("Good news — your order is on its way!")
        .build());
```

Email is the richest channel — it maps the full RFC 5322 field set: multiple `to`/`cc`/`bcc`
recipients, display names, `replyTo`, an optional HTML alternative, attachments, and custom headers:

```java
notifier.notify(EmailRequest.builder()
        .to(new EmailAddress("Alice", "alice@example.com"))   // or .to("alice@example.com")
        .cc("team@example.com")
        .bcc("audit@example.com")
        .from("shop@example.com")
        .replyTo("support@example.com")
        .subject("Your invoice")
        .body("Plain-text version")
        .htmlBody("<h1>Your invoice</h1><p>Thanks for your order.</p>")
        .attachment("invoice.pdf", pdfBytes, "application/pdf")
        .header("X-Campaign", "invoices")
        .build());
```

### Chat — Slack

```yaml
spring:
  notify:
    chat:
      slack:
        token: ${SLACK_BOT_TOKEN}   # xoxb-…
```

```java
notifier.notify(ChatRequest.builder()
        .to("#alerts")
        .message("Deploy finished :rocket:")
        .build());
```

---

## Provider-specific options

The request types carry only *portable* fields. Anything provider-specific rides in
`attributes`, which the sender reads by key:

```java
notifier.notify(PushRequest.builder()
        .to(deviceToken)
        .title("Sale")
        .body("50% off today")
        .attribute("badge", 1)
        .attribute("sound", "default")
        .build());
```

This keeps the common API clean while leaving an escape hatch for the full power of each SDK.

---

## Customizing in your app

Everything below is done from your own application — just declare beans.

### Use your own provider

Not using one of the bundled providers? Depend on the **channel** module instead of a
provider starter, and supply your own sender bean. The generic adapter picks it up:

```xml
<!-- the SMS channel, without a bundled provider -->
<dependency>
    <groupId>sk.solodev</groupId>
    <artifactId>notify-spring-boot-sms</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```java
@Component
class MyGatewaySmsSender implements SmsSender {   // SmsSender extends NotificationSender<SmsRequest>
    public String send(SmsRequest request) throws Exception {
        return myGateway.send(request.to(), request.from(), request.message()); // return the provider id
    }
}
```

That's it — `notifier.notify(SmsRequest…)` now routes to your sender. The same pattern
works for `PushSender`, `EmailSender`, and `ChatSender`.

> Install **one provider per channel**. Two senders for the same channel is a configuration
> error, reported at send time.

### Override a bundled provider

Every auto-configured bean backs off if you define your own. Want to customize the Twilio
`SmsSender`, or swap the SMTP `JavaMailSender`? Just declare a bean of that type and yours wins.

### Add cross-cutting behavior

Register a `NotificationInterceptor` bean to wrap **every** send — metrics, structured logging,
rate limiting, a feature-flag kill-switch. Order multiple with `@Order`:

```java
@Component
class LoggingInterceptor implements NotificationInterceptor {
    public String intercept(NotificationRequest request, Chain chain) {
        long start = System.nanoTime();
        String id = chain.proceed(request);          // or return early to short-circuit
        log.info("sent {} in {}ms", request.getClass().getSimpleName(), (System.nanoTime() - start) / 1_000_000);
        return id;
    }
}
```

To scope an interceptor to a single channel, extend `ChannelInterceptor<T>` — it runs only
for that request type and passes everything else straight through:

```java
@Component
class SmsRateLimiter extends ChannelInterceptor<SmsRequest> {
    SmsRateLimiter() { super(SmsRequest.class); }
    protected String interceptForChannel(SmsRequest request, Chain chain) {
        // runs only for SMS
        return chain.proceed(request);
    }
}
```

> **Retry, rate limiting, fallback** are yours to add here — wrap `chain.proceed(...)` with
> Spring's `RetryTemplate`, Resilience4j, or your own logic. The library deliberately doesn't
> impose a retry policy, because whether a resend is safe (idempotency) is your call, not the framework's.

## Observability

When an `ObservationRegistry` bean is present (add `spring-boot-starter-actuator`), every send is
wrapped in a Micrometer `Observation` automatically — one **timer**, one **tracing span**, and
structured **logs** per notification, tagged with the channel
(`notify.channel = sms | push | email | chat`). No configuration needed; without a registry bean
nothing is registered and sends are simply unobserved.

The observation is **delivery-scoped**: `spring.notify.send` times the provider call itself, not
your interceptors. A rate-limiter's wait or a retry wrapper's backoff falls *outside* the span, so
the timer reflects provider latency rather than the caller's total wait — which the enclosing
request or scheduled-task span already captures. Under a retry interceptor each attempt is its own
span. To measure the caller's total wait, read the enclosing span, not this one.

Customise the tags or name by declaring your own `NotificationObservationConvention` bean.

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
        deadLetterStore.save(event.request());   // e.g. queue for a later retry
    }
}
```

Both implement the sealed `NotificationEvent`, so a single listener can `switch` over them. Enabled
by default; set `spring.notify.events.enabled=false` to turn publication off.

---

## How it fits together

```
your code → Notifier.notify(request)
              → NotificationInterceptor chain      (ordered, optional)
              → AdapterResolver                    (picks the adapter by request type)
              → ChannelAdapter.deliver(request)    (wraps failures, returns message id)
                  → NotificationSender.send(request)   (the provider call: Twilio/FCM/SMTP/Slack)
```

- **`notify-core`** — the pipeline and contracts (`Notifier`, `NotificationRequest`,
  `ChannelAdapter`, `NotificationSender`, interceptors). No provider dependencies.
- **`notify-spring-boot-<channel>`** — a channel: its request type, `*Sender` SPI, and a generic adapter.
- **`notify-spring-boot-starter-<channel>-<provider>`** — one provider implementation you install.

You depend on a provider starter; everything below it comes transitively.

---

## Error handling

- **`NotificationDeliveryException`** (unchecked) — the provider failed to deliver. Carries
  `request()` (the request that failed) and the underlying cause. Catch it to retry, log, or fall back.
- **`IllegalStateException`** — a wiring problem (no provider installed for the request type, or
  more than one). This is a configuration bug to fix, not a runtime condition to handle.

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
