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
 * <p>Ordered innermost ({@link Ordered#LOWEST_PRECEDENCE}), so the observation measures the
 * <strong>delivery</strong> — adapter resolution and the provider SDK/network call — and nothing
 * else. Everything outside it is excluded: a rate-limiting interceptor's wait, a retry wrapper's
 * backoff, and {@code @EventListener} execution all fall outside the span, so {@code
 * spring.notify.send} reflects provider latency rather than the caller's total wait (which the
 * enclosing request or scheduled-task span already captures). Under a retry interceptor each
 * attempt is its own span; a short-circuit that never reaches the provider records nothing.
 *
 * @author Dominik Kovács
 * @since 1.0.0
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
        return Ordered.LOWEST_PRECEDENCE;
    }

    /** Derives the channel tag from the request type: {@code SmsRequest} → {@code sms}. */
    private static String channel(NotificationRequest request) {
        String name = request.getClass().getSimpleName();
        String base = name.endsWith("Request") ? name.substring(0, name.length() - "Request".length()) : name;
        return base.isEmpty() ? "unknown" : base.toLowerCase();
    }
}
