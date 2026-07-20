package sk.solodev.notify;

/**
 * Entry point for sending notifications. Consumers inject a {@code Notifier} and call
 * {@link #notify(NotificationRequest)} with a channel request (an {@code SmsRequest},
 * {@code EmailRequest}, …); the notifier routes it to the matching {@link ChannelAdapter},
 * applying any registered {@link NotificationInterceptor}s around the delivery.
 */
public interface Notifier {

    /**
     * Deliver a notification.
     *
     * @param request the channel request to send; its concrete type selects the channel
     * @return the provider message id of the accepted delivery
     * @throws IllegalStateException if no adapter, or more than one, handles the request type
     * @throws NotificationDeliveryException if the provider fails to deliver
     */
    String notify(NotificationRequest request);
}
