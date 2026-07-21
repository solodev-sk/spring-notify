package sk.solodev.notify.autoconfigure.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.core.Ordered;
import sk.solodev.notify.NotificationInterceptor;
import sk.solodev.notify.NotificationRequest;

/**
 * Wraps every send in a Micrometer {@link io.micrometer.observation.Observation}, yielding a
 * timer, a tracing span, and (with a logging handler) structured logs per notification. Ordered
 * outermost so it measures end-to-end latency and records every failure as {@code outcome=ERROR}.
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
