# Providers

Each provider is a starter you add for the backend you use. Install **exactly one provider per
channel** — two senders for the same channel is a wiring error reported at send time.

| Channel | Provider starter | Config prefix |
|---------|------------------|---------------|
| SMS   | `notify-spring-boot-starter-sms-twilio`     | `spring.notify.sms.twilio`   |
| SMS   | `notify-spring-boot-starter-sms-vonage`     | `spring.notify.sms.vonage`   |
| Push  | `notify-spring-boot-starter-push-fcm`       | `spring.notify.push.fcm`     |
| Push  | `notify-spring-boot-starter-push-apns`      | `spring.notify.push.apns`    |
| Email | `notify-spring-boot-starter-email-smtp`     | `spring.notify.email.smtp`   |
| Email | `notify-spring-boot-starter-email-sendgrid` | `spring.notify.email.sendgrid` |
| Chat  | `notify-spring-boot-starter-chat-slack`     | `spring.notify.chat.slack`   |

Each provider auto-configures itself only when its properties are present, and backs off if you
define your own `*Sender` bean.

## SMS

### Twilio

```yaml
spring:
  notify:
    sms:
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
```

### Vonage

```yaml
spring:
  notify:
    sms:
      vonage:
        api-key: ${VONAGE_API_KEY}
        api-secret: ${VONAGE_API_SECRET}
```

## Push

### Firebase Cloud Messaging (Android/web)

```yaml
spring:
  notify:
    push:
      fcm:
        service-account: ${FCM_SERVICE_ACCOUNT_JSON}   # the service-account JSON, inline
```

The starter configures a single `FirebaseMessaging` from one service account. If you need to send
to **several Firebase projects** (e.g. one per mobile app or per tenant), don't set the property
above — instead register your own `PushSender` that holds a `FirebaseMessaging` per project and
selects by a request attribute. Because the sender is `@ConditionalOnMissingBean`, yours replaces
the auto-configured one:

```java
@Component
class MultiAppFcmPushSender implements PushSender {

    private final Map<String, FirebaseMessaging> byApp;   // built once, one per FirebaseApp

    public String send(PushRequest request) throws Exception {
        var app = (String) request.attributes().get("app");   // caller picks the target project
        var messaging = byApp.get(app);
        Assert.notNull(messaging, () -> "no Firebase app configured for '" + app + "'");
        return new FcmPushSender(messaging).send(request);     // reuse the bundled sender's mapping
    }
}
```

```java
notifier.notify(PushRequest.builder()
        .to(deviceToken).title("Hi").body("…")
        .attribute("app", "storefront")   // routes to that project's FirebaseMessaging
        .build());
```

Initialise each `FirebaseApp` with a distinct name (`FirebaseApp.initializeApp(options, "storefront")`)
so they coexist. This is the same escape hatch as any custom sender — you wire the SDK instances
yourself, but reuse `FcmPushSender` for the request→FCM mapping.

### APNs (iOS, token auth)

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

## Email

### SMTP

```yaml
spring:
  notify:
    email:
      smtp:
        host: smtp.example.com
        port: 587                # defaults to 587 when unset
        username: ${SMTP_USERNAME}
        password: ${SMTP_PASSWORD}
```

### SendGrid

```yaml
spring:
  notify:
    email:
      sendgrid:
        api-key: ${SENDGRID_API_KEY}
```

## Chat

### Slack

```yaml
spring:
  notify:
    chat:
      slack:
        token: ${SLACK_BOT_TOKEN}   # xoxb-…
```

## Bringing your own provider

Not using a bundled provider? Depend on the **channel** module instead of a provider starter and
declare your own sender bean — the generic adapter picks it up:

```java
@Component
class MyGatewaySmsSender implements SmsSender {   // SmsSender extends NotificationSender<SmsRequest>
    public String send(SmsRequest request) throws Exception {
        return myGateway.send(request.to(), request.from(), request.message());
    }
}
```

That's it — `notifier.notify(SmsRequest…)` now routes to your sender. The same pattern works for
`PushSender`, `EmailSender`, and `ChatSender`.

## Multiple providers for one channel

Not supported yet. The framework routes by request type and wires **one** provider per channel, so
installing two starters for the same channel (say Twilio *and* Vonage) does not give you a choice
between them — the provider auto-configurations back off one another, and which one wins is
undefined. Don't install more than one starter per channel.

First-class multi-provider support (failover, per-region routing) is on the roadmap. Until then, if
you need it today, the escape hatch is to skip the bundled starters for that channel and register
your own `SmsSender` that wraps the provider SDKs and picks between them — but note that means
wiring those SDK clients yourself, without the auto-configuration the starters provide.

---

Next: [Observability & events](observability-and-events.md) — metrics, tracing, and lifecycle events.
