package sk.solodev.notify.email;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** An email address with an optional display name (e.g. {@code "Alice" <alice@example.com>}). */
public record EmailAddress(@Nullable String name, String address) {

    public EmailAddress {
        Objects.requireNonNull(address, "address must not be null");
    }

    /** An address with no display name. */
    public static EmailAddress of(String address) {
        return new EmailAddress(null, address);
    }
}
