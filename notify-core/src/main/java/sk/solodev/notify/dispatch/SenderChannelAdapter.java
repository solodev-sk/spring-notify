package sk.solodev.notify.dispatch;

import sk.solodev.notify.NotificationDeliveryException;
import sk.solodev.notify.NotificationRequest;
import sk.solodev.notify.NotificationSender;

/**
 * Generic {@link ChannelAdapter} for the common case: route requests of one concrete type
 * to a {@link NotificationSender}, returning the provider message id and normalising any
 * failure to a {@link NotificationDeliveryException}. Channel modules register one of these
 * instead of hand-writing an adapter per channel.
 *
 * @param <T> the channel request type this adapter handles
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public final class SenderChannelAdapter<T extends NotificationRequest> implements ChannelAdapter<T> {

    private final Class<T> requestType;

    private final NotificationSender<T> sender;

    /**
     * @param requestType the concrete request type this adapter routes (used by {@link #supports})
     * @param sender      the provider sender to delegate delivery to
     */
    public SenderChannelAdapter(Class<T> requestType, NotificationSender<T> sender) {
        this.requestType = requestType;
        this.sender = sender;
    }

    @Override
    public boolean supports(NotificationRequest request) {
        return requestType.isInstance(request);
    }

    @Override
    public String deliver(T request) {
        try {
            return sender.send(request);
        }
        catch (Exception ex) {
            throw new NotificationDeliveryException(
                    "Delivery via " + requestType.getSimpleName() + " failed", request, ex);
        }
    }
}
