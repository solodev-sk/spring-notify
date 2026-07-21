package sk.solodev.notify.event;

import sk.solodev.notify.NotificationRequest;

/**
 * An event published for every completed send attempt: {@link NotificationSent} on success,
 * {@link NotificationFailed} on failure. Applications observe these with Spring's
 * {@code @EventListener} for audit trails, dead-lettering, or their own retry — without wrapping
 * the send. The types are plain data and carry no Spring dependency; the framework publishes them
 * via Spring's {@code ApplicationEventPublisher}.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public sealed interface NotificationEvent permits NotificationSent, NotificationFailed {

    /** The request this event concerns. */
    NotificationRequest request();
}
