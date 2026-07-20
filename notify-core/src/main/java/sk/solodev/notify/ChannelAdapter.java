package sk.solodev.notify;

/**
 * Sends a notification over one channel. Implemented by channel modules and
 * registered as a Spring bean. {@code T} is the canonical request type this
 * adapter handles.
 */
public interface ChannelAdapter<T extends NotificationRequest> {

    boolean supports(NotificationRequest request);

    /** Deliver the request; returns the provider message id. */
    String deliver(T request);
}
