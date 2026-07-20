package sk.solodev.notify;

/** Minimal {@link NotificationRequest} for core pipeline tests — core owns no concrete request type. */
public record TestRequest(String id) implements NotificationRequest {
}
