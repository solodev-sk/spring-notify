package sk.solodev.notify.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * {@link ObservationConvention} for notification sends. Register a bean implementing this
 * interface to customise the observation name or tags without replacing the interceptor.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public interface NotificationObservationConvention extends ObservationConvention<NotificationObservationContext> {

    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof NotificationObservationContext;
    }
}
