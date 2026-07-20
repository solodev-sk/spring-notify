package sk.solodev.notify.chat;

import sk.solodev.notify.NotificationRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical chat request. {@code to} is a provider-interpreted destination (a Slack
 * channel id like {@code "#alerts"}, a Discord channel id, …); {@code message} is the
 * text. Provider-specific rich content (Slack blocks, Discord embeds, …) travels in
 * {@code attributes}. Nullness is a compile-time contract (jspecify); there is no runtime
 * validation beyond {@code attributes} immutability normalization.
 */
public record ChatRequest(String to, String message,
                          Map<String, Object> attributes) implements NotificationRequest {

    public ChatRequest {
        attributes = Map.copyOf(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String to;

        private String message;

        private final Map<String, Object> attributes = new HashMap<>();

        private Builder() {

        }

        public Builder to(String to) {
            this.to = to;
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

        public ChatRequest build() {
            return new ChatRequest(
                    Objects.requireNonNull(to, "to must be set"),
                    Objects.requireNonNull(message, "message must be set"),
                    attributes);
        }
    }
}
