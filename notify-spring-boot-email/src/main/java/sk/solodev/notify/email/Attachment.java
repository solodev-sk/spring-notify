package sk.solodev.notify.email;

import java.util.Objects;

/** An email attachment: a file name, its raw bytes, and the MIME content type. */
public record Attachment(String filename, byte[] content, String contentType) {

    public Attachment {
        Objects.requireNonNull(filename, "filename must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
    }
}
