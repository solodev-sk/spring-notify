package sk.solodev.notify.email;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * An email attachment: a file name, its raw bytes, and the MIME content type.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public record Attachment(String filename, byte[] content, String contentType) {

    public Attachment {
        Objects.requireNonNull(filename, "filename must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private @Nullable String filename;

        private byte @Nullable [] content;

        private @Nullable String contentType;

        private Builder() {

        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder content(byte[] content) {
            this.content = content;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Attachment build() {
            return new Attachment(filename, content, contentType);
        }
    }
}
