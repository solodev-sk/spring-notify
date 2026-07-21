package sk.solodev.notify.observation;

import io.micrometer.common.KeyValues;

import static sk.solodev.notify.observation.NotificationObservationDocumentation.LowCardinalityKeys.CHANNEL;

/**
 * Default {@link NotificationObservationConvention}: names the observation
 * {@code spring.notify.send} and tags it with the channel. Only low-cardinality values
 * are recorded — recipient, message id, and body are deliberately never tagged.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public class DefaultNotificationObservationConvention implements NotificationObservationConvention {

    @Override
    public String getName() {
        return NotificationObservationDocumentation.SEND.getName();
    }

    @Override
    public String getContextualName(NotificationObservationContext context) {
        return "notify " + context.channel();
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(NotificationObservationContext context) {
        return KeyValues.of(CHANNEL.withValue(context.channel()));
    }
}
