package sk.solodev.notify.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.core.Ordered;
import sk.solodev.notify.NotificationRequest;
import sk.solodev.notify.interceptor.NotificationInterceptor;

/**
 * Wraps every send in a Micrometer {@link io.micrometer.observation.Observation}, yielding a
 * timer, a tracing span, and (with a logging handler) structured logs per notification.
 *
 * <p>Ordered outermost ({@link Ordered#HIGHEST_PRECEDENCE}), so the observation measures the
 * <strong>entire</strong> send: any lower-ordered interceptors, adapter resolution, and the
 * provider SDK/network call — not just the provider call in isolation. Every failure, from any
 * layer, is recorded as {@code outcome=ERROR}. A slow or blocking interceptor (e.g. rate limiting)
 * is therefore included in the timing, which reflects the caller's true wait. To measure provider
 * latency alone, add a separate observation inside the sender.
 */
public class ObservationNotificationInterceptor implements NotificationInterceptor, Ordered {

    private final ObservationRegistry registry;

    private final NotificationObservationConvention convention;

    public ObservationNotificationInterceptor(ObservationRegistry registry,
                                              NotificationObservationConvention convention) {
        this.registry = registry;
        this.convention = convention;
    }

    @Override
    public String intercept(NotificationRequest request, Chain chain) {
        var context = new NotificationObservationContext(request, channel(request));
        return Observation.createNotStarted(convention, () -> context, registry)
                .observe(() -> chain.proceed(request));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /** Derives the channel tag from the request type: {@code SmsRequest} → {@code sms}. */
    private static String channel(NotificationRequest request) {
        String name = request.getClass().getSimpleName();
        String base = name.endsWith("Request") ? name.substring(0, name.length() - "Request".length()) : name;
        return base.isEmpty() ? "unknown" : base.toLowerCase();
    }
}
