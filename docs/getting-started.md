# Getting started

## 1. Build it locally

spring-notify is not yet on Maven Central. Build it into your local `~/.m2`:

```bash
git clone https://github.com/solodev-sk/spring-notify.git
cd spring-notify
./mvnw install
```

## 2. Import the BOM (recommended)

If you depend on more than one `notify-*` artifact, import the BOM once so every module resolves
to the same version and you can omit `<version>` everywhere:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>sk.solodev</groupId>
            <artifactId>notify-bom</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 3. Add a provider starter

Pick the backend you want. The starter pulls in the channel and the whole pipeline transitively:

```xml
<dependency>
    <groupId>sk.solodev</groupId>
    <artifactId>notify-spring-boot-starter-sms-twilio</artifactId>
</dependency>
```

## 4. Configure it

```yaml
spring:
  notify:
    sms:
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
```

Config keys autocomplete in your IDE — every provider's keys ship with generated metadata.

## 5. Inject `Notifier` and send

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

`notify(...)` returns the provider message id, or throws `NotificationDeliveryException` (carrying
the failed request) on failure. To send off the calling thread, use `notifyAsync(...)` — see
[Concepts](concepts.md#asynchronous-sending).

---

Next: [Concepts](concepts.md) — understand the pipeline. Or jump to
[Providers](providers.md) for backend configuration, or [Testing](testing.md) to assert sends
without a real provider.
