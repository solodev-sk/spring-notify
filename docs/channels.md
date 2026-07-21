# Channels

Each channel defines an immutable request type built with a fluent builder. Required fields are
validated at `build()` time (missing or blank → `IllegalArgumentException`). Anything
provider-specific that isn't a portable field goes in `attributes` (or `headers` for email), which
the sender reads by key.

## SMS — `SmsRequest`

| Field | Required | Notes |
|-------|----------|-------|
| `to` | yes | recipient phone number |
| `from` | yes | sender number (no channel default; you supply it) |
| `message` | yes | the text |
| `attributes` | no | provider extras (media URLs, messaging-service SID, …) |

```java
SmsRequest.builder()
        .to("+421900123456")
        .from("+421900999888")
        .message("Hello from spring-notify")
        .build();
```

## Push — `PushRequest`

| Field | Required | Notes |
|-------|----------|-------|
| `to` | yes | device token |
| `title` | yes | notification title |
| `body` | yes | notification body |
| `attributes` | no | data payload, sound, badge, topic, … (FCM data / APNs custom payload) |

```java
PushRequest.builder()
        .to(deviceToken)
        .title("Order shipped")
        .body("Your order is on its way")
        .attribute("orderId", "12345")
        .build();
```

## Email — `EmailRequest`

The richest channel — the standard RFC 5322 field set.

| Field | Required | Notes |
|-------|----------|-------|
| `to` | yes (≥1) | `List<EmailAddress>`; add via `.to("a@b.com")` or `.to(new EmailAddress("Alice", "a@b.com"))` |
| `cc` / `bcc` | no | additional recipients |
| `from` | yes | sender address |
| `replyTo` | no | reply-to address |
| `subject` | yes | non-blank |
| `body` | yes | plain-text body, non-blank |
| `htmlBody` | no | HTML alternative (both present → `multipart/alternative`) |
| `attachments` | no | `Attachment(filename, byte[], contentType)` |
| `headers` | no | custom headers |
| `attributes` | no | provider extras |

```java
EmailRequest.builder()
        .to(new EmailAddress("Alice", "alice@example.com"))
        .cc("team@example.com")
        .from("shop@example.com")
        .replyTo("support@example.com")
        .subject("Your invoice")
        .body("Plain-text version")
        .htmlBody("<h1>Your invoice</h1><p>Thanks for your order.</p>")
        .attachments(Attachment.builder()
                .filename("invoice.pdf").content(pdfBytes).contentType("application/pdf")
                .build())
        .header("X-Campaign", "invoices")
        .build();
```

## Chat — `ChatRequest`

| Field | Required | Notes |
|-------|----------|-------|
| `to` | yes | provider-interpreted destination (a Slack channel like `#alerts`, …) |
| `message` | yes | the text |
| `attributes` | no | rich content (Slack blocks, …) |

```java
ChatRequest.builder()
        .to("#alerts")
        .message("Deploy finished :rocket:")
        .build();
```

---

Next: [Providers](providers.md) — the provider matrix and per-provider configuration.
