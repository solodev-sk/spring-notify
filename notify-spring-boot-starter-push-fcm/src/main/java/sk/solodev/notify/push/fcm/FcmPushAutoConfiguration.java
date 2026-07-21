package sk.solodev.notify.push.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.push.PushAutoConfiguration;
import sk.solodev.notify.push.PushSender;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Registers an FCM-backed {@link PushSender} when
 * {@code spring.notify.push.fcm.service-account} is set. Runs before
 * {@link PushAutoConfiguration} so the sender bean exists when that config's
 * {@code @ConditionalOnBean(PushSender.class)} adapter is evaluated.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@AutoConfiguration(before = PushAutoConfiguration.class)
@EnableConfigurationProperties(FcmProperties.class)
@ConditionalOnProperty(prefix = "spring.notify.push.fcm", name = "service-account")
public class FcmPushAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FirebaseApp notifyFirebaseApp(FcmProperties properties) throws IOException {
        var options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(properties.serviceAccount().getBytes(StandardCharsets.UTF_8))))
                .build();
        return FirebaseApp.initializeApp(options, "spring-notify");
    }

    @Bean
    @ConditionalOnMissingBean
    public FirebaseMessaging notifyFirebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }

    @Bean
    @ConditionalOnMissingBean
    public PushSender fcmPushSender(FirebaseMessaging messaging) {
        return new FcmPushSender(messaging);
    }
}
