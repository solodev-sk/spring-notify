package sk.solodev.notify.push.apns;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * APNs configuration (token-based auth). Configured under {@code spring.notify.push.apns}.
 *
 * @param signingKey         the {@code .p8} signing key contents (APNs auth key)
 * @param keyId              the key id of the signing key
 * @param teamId             the Apple developer team id
 * @param topic              the app bundle id every push targets
 * @param production         {@code true} for the production APNs gateway, {@code false} for the sandbox
 * @param host               overrides the APNs gateway host; leave unset for Apple's real gateways
 *                           (selected by {@code production}). Set it to point at a local mock server in tests.
 * @param port               the gateway port; defaults to {@code 443} when unset
 * @param trustedCertificate a PEM file with the server certificate chain to trust instead of the JDK
 *                           default trust store; e.g. a corporate TLS-intercepting proxy's CA. Leave
 *                           unset to trust Apple's publicly-signed gateways via the default trust store.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@ConfigurationProperties("spring.notify.push.apns")
public record ApnsProperties(String signingKey, String keyId, String teamId, String topic, boolean production,
                             @Nullable String host, int port, @Nullable Resource trustedCertificate) {

    public ApnsProperties {
        port = port == 0 ? 443 : port;
    }
}
