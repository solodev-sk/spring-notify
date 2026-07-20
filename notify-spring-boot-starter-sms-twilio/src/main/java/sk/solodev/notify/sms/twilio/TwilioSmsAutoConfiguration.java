package sk.solodev.notify.sms.twilio;

import com.twilio.http.TwilioRestClient;
import sk.solodev.notify.sms.SmsAutoConfiguration;
import sk.solodev.notify.sms.SmsSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Registers a Twilio-backed {@link SmsSender} when
 * {@code spring.notify.sms.twilio.account-sid} is set. Runs before
 * {@link SmsAutoConfiguration} so the sender bean exists when that config's
 * {@code @ConditionalOnBean(SmsSender.class)} adapter is evaluated.
 */
@AutoConfiguration(before = SmsAutoConfiguration.class)
@EnableConfigurationProperties(TwilioSmsProperties.class)
@ConditionalOnProperty(prefix = "spring.notify.sms.twilio", name = "account-sid")
public class TwilioSmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TwilioRestClient twilioRestClient(TwilioSmsProperties properties) {
        return new TwilioRestClient.Builder(properties.accountSid(), properties.authToken()).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SmsSender twilioSmsSender(TwilioRestClient restClient) {
        return new TwilioSmsSender(restClient);
    }
}
