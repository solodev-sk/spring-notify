package sk.solodev.notify.observation;

import io.micrometer.observation.Observation;
import sk.solodev.notify.NotificationRequest;

/**
 * {@link Observation.Context} for a single notification send. Carries the request and its
 * derived channel name so an {@link NotificationObservationConvention} can build the tags.
 */
public class NotificationObservationContext extends Observation.Context {

    private final NotificationRequest request;

    private final String channel;

    public NotificationObservationContext(NotificationRequest request, String channel) {
        this.request = request;
        this.channel = channel;
    }

    public NotificationRequest request() {
        return request;
    }

    public String channel() {
        return channel;
    }
}
