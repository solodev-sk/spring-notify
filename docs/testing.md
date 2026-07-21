# Testing

The `notify-test` module provides `RecordingNotifier` — a `Notifier` double that captures sends
instead of delivering them, so you can assert what *would* be sent without a real provider.

```xml
<dependency>
    <groupId>sk.solodev</groupId>
    <artifactId>notify-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Basic use

Inject it wherever a `Notifier` is expected, exercise your code, then assert:

```java
var notifier = new RecordingNotifier();
var orders = new OrderService(notifier);

orders.onShipped(order);

assertThat(notifier.sent(SmsRequest.class)).hasSize(1);
assertThat(notifier.lastSent()).contains(expectedRequest);
```

## What it records and returns

- `notify(...)` records the request and returns a message id — `"test-message-id"` by default.
- `notifyAsync(...)` runs **synchronously** and returns an already-completed future, so async sends
  are deterministic in tests.

## Query methods

| Method | Returns |
|--------|---------|
| `sent()` | all recorded requests, in order |
| `sent(Type.class)` | recorded requests of that type, in order |
| `lastSent()` | `Optional` of the most recent request |
| `count()` | number recorded |
| `nothingSent()` | `true` if none recorded |
| `clear()` | forget all recorded requests |

## Controlling behaviour

- `returning("id")` / `returning(request -> ...)` — set the message id `notify` returns.
- `failWith(exception)` — record the request, then throw (surfaces through `notifyAsync` as a
  failed future). Use it to exercise your error handling.

```java
notifier.failWith(new NotificationDeliveryException("down", request, cause));

assertThatThrownBy(() -> orders.onShipped(order))
        .isInstanceOf(NotificationDeliveryException.class);
```

`RecordingNotifier` is not thread-safe; it is intended for single-threaded test use.
