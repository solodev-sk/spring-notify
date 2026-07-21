package sk.solodev.notify.push.apns;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.push.PushAutoConfiguration;
import sk.solodev.notify.push.PushSender;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Registers an APNs-backed {@link PushSender} when {@code spring.notify.push.apns.signing-key} is
 * set. Uses token (.p8) authentication. Runs before {@link PushAutoConfiguration} so the sender
 * bean exists when that config's {@code @ConditionalOnBean(PushSender.class)} adapter is evaluated.
 */
@AutoConfiguration(before = PushAutoConfiguration.class)
@EnableConfigurationProperties(ApnsProperties.class)
@ConditionalOnProperty(prefix = "spring.notify.push.apns", name = "signing-key")
public class ApnsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApnsClient apnsClient(ApnsProperties properties) throws Exception {
        var signingKey = ApnsSigningKey.loadFromInputStream(
                new ByteArrayInputStream(properties.signingKey().getBytes(StandardCharsets.UTF_8)),
                properties.teamId(), properties.keyId());
        return new ApnsClientBuilder()
                .setApnsServer(properties.production()
                        ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                        : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                .setSigningKey(signingKey)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public PushSender apnsPushSender(ApnsClient apnsClient, ApnsProperties properties) {
        return new ApnsPushSender(apnsClient, properties.topic());
    }
}
