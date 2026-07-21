package sk.solodev.notify;

/**
 * Published when a notification fails to deliver.
 *
 * @param request the request that failed
 * @param cause   the failure — a {@link NotificationDeliveryException} for a provider failure,
 *                or another exception thrown from the pipeline
 */
public record NotificationFailed(NotificationRequest request, Throwable cause) implements NotificationEvent {

}
