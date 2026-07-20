package sk.solodev.notify.push.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FCM configuration. {@code serviceAccount} is the service-account credentials JSON
 * (the content itself), used to initialise the Firebase Admin SDK.
 */
@ConfigurationProperties("spring.notify.push.fcm")
public record FcmProperties(String serviceAccount) {
}
