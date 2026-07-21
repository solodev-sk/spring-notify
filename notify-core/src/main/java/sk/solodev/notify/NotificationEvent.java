package sk.solodev.notify;

/**
 * An event published for every completed send attempt: {@link NotificationSent} on success,
 * {@link NotificationFailed} on failure. Applications observe these with Spring's
 * {@code @EventListener} for audit trails, dead-lettering, or their own retry — without wrapping
 * the send. The types are plain data and carry no Spring dependency; the framework publishes them
 * via Spring's {@code ApplicationEventPublisher}.
 */
public sealed interface NotificationEvent permits NotificationSent, NotificationFailed {

    /** The request this event concerns. */
    NotificationRequest request();
}
