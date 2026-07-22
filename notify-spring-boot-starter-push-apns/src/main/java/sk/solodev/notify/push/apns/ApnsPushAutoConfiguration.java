package sk.solodev.notify.push.apns;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import org.springframework.beans.factory.ObjectProvider;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;

/**
 * Registers an APNs-backed {@link PushSender} when {@code spring.notify.push.apns.signing-key} is
 * set. Uses token (.p8) authentication. Runs before {@link PushAutoConfiguration} so the sender
 * bean exists when that config's {@code @ConditionalOnBean(PushSender.class)} adapter is evaluated.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@AutoConfiguration(before = PushAutoConfiguration.class)
@EnableConfigurationProperties(ApnsProperties.class)
@ConditionalOnProperty(prefix = "spring.notify.push.apns", name = "signing-key")
public class ApnsPushAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApnsClient apnsClient(ApnsProperties properties) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        var signingKey = ApnsSigningKey.loadFromInputStream(
                new ByteArrayInputStream(properties.signingKey().getBytes(StandardCharsets.UTF_8)),
                properties.teamId(), properties.keyId());
        var builder = new ApnsClientBuilder()
                .setApnsServer(resolveHost(properties), properties.port())
                .setSigningKey(signingKey);
        if (properties.trustedCertificate() == null) {
            return builder.build();
        }
        // pushy reads the chain lazily at build(), so keep the stream open until then
        try (var chain = properties.trustedCertificate().getInputStream()) {
            return builder.setTrustedServerCertificateChain(chain).build();
        }
    }

    private static String resolveHost(ApnsProperties properties) {
        var host = properties.host();
        if (host != null) {
            return host;
        } else if (properties.production()) {
            return ApnsClientBuilder.PRODUCTION_APNS_HOST;
        }
        else {
            return ApnsClientBuilder.DEVELOPMENT_APNS_HOST;
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public PushSender apnsPushSender(ApnsClient apnsClient, ApnsProperties properties,
                                     ObjectProvider<Clock> clock) {
        return new ApnsPushSender(apnsClient, properties.topic(),
                clock.getIfAvailable(Clock::systemDefaultZone));
    }
}
