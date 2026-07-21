package sk.solodev.notify.push.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FCM configuration. Configured under {@code spring.notify.push.fcm}.
 *
 * @param serviceAccount the service-account credentials JSON (the content itself), used to
 *                       initialise the Firebase Admin SDK
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@ConfigurationProperties("spring.notify.push.fcm")
public record FcmProperties(String serviceAccount) {
}
