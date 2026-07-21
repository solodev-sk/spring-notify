# Adding a provider

A provider is a thin starter that backs one channel with a concrete service. Adding one is a
five-file pattern — no changes to core or the channel module. This walks through it using an
imaginary `Acme` SMS provider; the SMS channel already exists, so we only implement against its
`SmsSender` SPI.

The bundled providers are the reference implementations — the SMS ones (`sms-twilio`, `sms-vonage`)
are the smallest.

## 1. Module and POM

Create `notify-spring-boot-starter-sms-acme/pom.xml`. It depends on the **channel** module, the
provider SDK, and the config-metadata processor:

```xml
<dependencies>
    <dependency>
        <groupId>sk.solodev</groupId>
        <artifactId>notify-spring-boot-sms</artifactId>
    </dependency>
    <dependency>
        <groupId>org.jspecify</groupId>
        <artifactId>jspecify</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>com.acme</groupId>
        <artifactId>acme-sdk</artifactId>   <!-- version managed in the parent POM -->
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Register the module in the parent `<modules>`, its artifact in `notify-bom`, and the SDK version in
the parent's `dependencyManagement` (with a version property).

## 2. Properties

Document every component with `@param` so the config keys carry IDE descriptions:

```java
/**
 * Acme credentials. Configured under {@code spring.notify.sms.acme}.
 *
 * @param apiKey the Acme API key
 */
@ConfigurationProperties("spring.notify.sms.acme")
public record AcmeSmsProperties(String apiKey) {
}
```

## 3. Sender

Implement the channel SPI. Return the provider's message id; let failures propagate — the adapter
wraps them in `NotificationDeliveryException`. Throw a plain `RuntimeException` if the provider
reports a non-success response.

```java
public class AcmeSmsSender implements SmsSender {

    private final AcmeClient client;

    public AcmeSmsSender(AcmeClient client) {
        this.client = client;
    }

    @Override
    public String send(SmsRequest request) throws Exception {
        var response = client.sendSms(request.from(), request.to(), request.message());
        if (!response.ok()) {
            throw new RuntimeException("Acme rejected the message: " + response.error());
        }
        return response.id();
    }
}
```

## 4. Auto-configuration

Gate on a property, run **before** the channel auto-config so the sender bean exists when the
channel's `@ConditionalOnBean(SmsSender.class)` adapter is evaluated, and back off if the app
defines its own bean:

```java
@AutoConfiguration(before = SmsAutoConfiguration.class)
@EnableConfigurationProperties(AcmeSmsProperties.class)
@ConditionalOnProperty(prefix = "spring.notify.sms.acme", name = "api-key")
public class AcmeSmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SmsSender acmeSmsSender(AcmeSmsProperties properties) {
        return new AcmeSmsSender(new AcmeClient(properties.apiKey()));
    }
}
```

> The `before = SmsAutoConfiguration.class` ordering matters: without it the channel adapter can be
> evaluated before your sender bean is registered, and the adapter silently backs off.

## 5. Register the auto-configuration

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
sk.solodev.notify.sms.acme.AcmeSmsAutoConfiguration
```

Add a `package-info.java` with `@NullMarked` (every package is null-marked).

## 6. Test

Mock the SDK client and assert the mapping and the returned id — see `VonageSmsSenderTest` for the
pattern (mock the client; verify the request is mapped and the id returned; assert a rejected
response throws).

## Checklist

- [ ] Module registered in parent `<modules>` and `notify-bom`
- [ ] SDK version in parent `dependencyManagement` + a version property
- [ ] `*Properties` with `@param`-documented components
- [ ] `*Sender implements <Channel>Sender`, returns the message id, throws on provider failure
- [ ] `*AutoConfiguration` with `@AutoConfiguration(before = <Channel>AutoConfiguration.class)`,
      `@ConditionalOnProperty`, `@ConditionalOnMissingBean`
- [ ] `.imports` file and `@NullMarked` `package-info`
- [ ] A sender test
