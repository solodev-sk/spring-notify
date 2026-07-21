package sk.solodev.notify;

/**
 * Signals that an adapter failed to deliver a request. Carries the failed {@link #request()}.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public class NotificationDeliveryException extends RuntimeException {

    private final transient NotificationRequest request;

    public NotificationDeliveryException(String message, NotificationRequest request, Throwable cause) {
        super(message, cause);
        this.request = request;
    }

    /** The request whose delivery failed. */
    public NotificationRequest request() {
        return request;
    }
}
