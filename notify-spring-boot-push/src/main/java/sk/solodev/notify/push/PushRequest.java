package sk.solodev.notify.push;

import sk.solodev.notify.NotificationRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical push request. {@code to} is the device token; {@code title}/{@code body}
 * are the notification content. Provider-specific options (data payload, sound, badge,
 * topic, …) travel in {@code attributes}. Nullness is a compile-time contract (jspecify);
 * there is no runtime validation beyond {@code attributes} immutability normalisation.
 */
public record PushRequest(String to, String title, String body,
                          Map<String, Object> attributes) implements NotificationRequest {

    public PushRequest {
        attributes = Map.copyOf(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String to;

        private String title;

        private String body;

        private final Map<String, Object> attributes = new HashMap<>();

        private Builder() {
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder attribute(String key, Object value) {
            attributes.put(key, value);
            return this;
        }

        public PushRequest build() {
            return new PushRequest(
                    Objects.requireNonNull(to, "to must be set"),
                    Objects.requireNonNull(title, "title must be set"),
                    Objects.requireNonNull(body, "body must be set"),
                    attributes);
        }
    }
}
