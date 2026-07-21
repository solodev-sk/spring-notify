package sk.solodev.notify.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import sk.solodev.notify.NotificationDeliveryException;
import sk.solodev.notify.NotificationRequest;
import sk.solodev.notify.interceptor.NotificationInterceptor;

/**
 * Publishes a {@link NotificationSent} after a successful send and a {@link NotificationFailed}
 * (then rethrows) on failure, letting applications react with {@code @EventListener} — audit
 * trails, dead-lettering, app-side retry — without wrapping the send.
 *
 * <p>Ordered innermost ({@link Ordered#LOWEST_PRECEDENCE}), as close to the provider call as
 * possible, so events reflect the <strong>actual</strong> delivery: any other interceptor (retry,
 * rate limiting, a short-circuit) sits outside it. A short-circuit that returns without delivering
 * therefore publishes no event, a user retry wrapper sees one {@link NotificationFailed} per real
 * attempt, and the failure carried is the {@link NotificationDeliveryException} the adapter throws.
 */
public class EventPublishingNotificationInterceptor implements NotificationInterceptor, Ordered {

    private final ApplicationEventPublisher publisher;

    public EventPublishingNotificationInterceptor(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public String intercept(NotificationRequest request, Chain chain) {
        try {
            String messageId = chain.proceed(request);
            publisher.publishEvent(new NotificationSent(request, messageId));
            return messageId;
        }
        catch (RuntimeException ex) {
            publisher.publishEvent(new NotificationFailed(request, ex));
            throw ex;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
