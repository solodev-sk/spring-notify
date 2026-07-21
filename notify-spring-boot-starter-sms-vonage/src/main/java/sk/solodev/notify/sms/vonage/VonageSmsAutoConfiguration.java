package sk.solodev.notify.sms.vonage;

import com.vonage.client.VonageClient;
import com.vonage.client.sms.SmsClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.sms.SmsAutoConfiguration;
import sk.solodev.notify.sms.SmsSender;

/**
 * Registers a Vonage-backed {@link SmsSender} when
 * {@code spring.notify.sms.vonage.api-key} is set. Runs before
 * {@link SmsAutoConfiguration} so the sender bean exists when that config's
 * {@code @ConditionalOnBean(SmsSender.class)} adapter is evaluated.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@AutoConfiguration(before = SmsAutoConfiguration.class)
@EnableConfigurationProperties(VonageProperties.class)
@ConditionalOnProperty(prefix = "spring.notify.sms.vonage", name = "api-key")
public class VonageSmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SmsClient vonageSmsClient(VonageProperties properties) {
        return VonageClient.builder()
                .apiKey(properties.apiKey())
                .apiSecret(properties.apiSecret())
                .build()
                .getSmsClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public SmsSender vonageSmsSender(SmsClient smsClient) {
        return new VonageSmsSender(smsClient);
    }
}
