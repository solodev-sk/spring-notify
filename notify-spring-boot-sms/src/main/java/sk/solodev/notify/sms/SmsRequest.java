package sk.solodev.notify.sms;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import sk.solodev.notify.NotificationRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Canonical SMS request. {@code from} is the sender and is supplied by the consumer
 * (there is no channel-level default). Provider-specific options (media URLs,
 * messaging-service SID, …) travel in {@code attributes}. Nullness is a compile-time
 * contract (jspecify); {@code build()} additionally rejects blank {@code to}, {@code from},
 * and {@code message}.
 */
public record SmsRequest(String to, String from, String message,
                         Map<String, Object> attributes) implements NotificationRequest {

    public SmsRequest {
        attributes = Map.copyOf(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private @Nullable String to;

        private @Nullable String from;

        private @Nullable String message;

        private final Map<String, Object> attributes = new HashMap<>();

        private Builder() {
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder attribute(String key, Object value) {
            attributes.put(key, value);
            return this;
        }

        public SmsRequest build() {
            Assert.hasText(to, "to must be set");
            Assert.hasText(from, "from must be set");
            Assert.hasText(message, "message must be set");
            return new SmsRequest(to, from, message, attributes);
        }
    }
}
