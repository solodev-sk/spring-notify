package sk.solodev.notify.email;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * An email address with an optional display name (e.g. {@code "Alice" <alice@example.com>}).
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public record EmailAddress(@Nullable String name, String address) {

    public EmailAddress {
        Assert.hasText(address, "address must be set");
    }

    /** An address with no display name. */
    public static EmailAddress of(String address) {
        return new EmailAddress(null, address);
    }
}
