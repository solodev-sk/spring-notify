package sk.solodev.notify.event;

import sk.solodev.notify.NotificationDeliveryException;
import sk.solodev.notify.NotificationRequest;

/**
 * Published when a notification fails to deliver.
 *
 * @param request the request that failed
 * @param cause   the failure — a {@link NotificationDeliveryException} for a provider failure,
 *                or another exception thrown from the pipeline
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public record NotificationFailed(NotificationRequest request, Throwable cause) implements NotificationEvent {

}
