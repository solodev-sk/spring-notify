package sk.solodev.notify.push.apns;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * APNs configuration (token-based auth). Configured under {@code spring.notify.push.apns}.
 *
 * @param signingKey the {@code .p8} signing key contents (APNs auth key)
 * @param keyId      the key id of the signing key
 * @param teamId     the Apple developer team id
 * @param topic      the app bundle id every push targets
 * @param production {@code true} for the production APNs gateway, {@code false} for the sandbox
 */
@ConfigurationProperties("spring.notify.push.apns")
public record ApnsProperties(String signingKey, String keyId, String teamId, String topic, boolean production) {
}
