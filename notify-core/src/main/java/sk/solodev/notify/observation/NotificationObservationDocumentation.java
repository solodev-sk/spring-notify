package sk.solodev.notify.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Documents the {@code spring.notify.send} observation: its default name and the
 * low-cardinality keys it records. Used to generate metric/span reference docs.
 */
public enum NotificationObservationDocumentation implements ObservationDocumentation {

    SEND {
        @Override
        public String getName() {
            return "spring.notify.send";
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityKeys.values();
        }
    };

    public enum LowCardinalityKeys implements KeyName {

        /** The notification channel: {@code sms}, {@code push}, {@code email}, {@code chat}, … */
        CHANNEL {
            @Override
            public String asString() {
                return "notify.channel";
            }
        }
    }
}
