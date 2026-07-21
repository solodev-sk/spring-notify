package sk.solodev.notify.email.sendgrid;

import com.sendgrid.SendGrid;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.email.EmailAutoConfiguration;
import sk.solodev.notify.email.EmailSender;

/**
 * Registers a SendGrid-backed {@link EmailSender} when {@code spring.notify.email.sendgrid.api-key}
 * is set. Runs before {@link EmailAutoConfiguration} so the sender bean exists when that config's
 * {@code @ConditionalOnBean(EmailSender.class)} adapter is evaluated.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@AutoConfiguration(before = EmailAutoConfiguration.class)
@EnableConfigurationProperties(SendGridProperties.class)
@ConditionalOnProperty(prefix = "spring.notify.email.sendgrid", name = "api-key")
public class SendGridEmailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SendGrid sendGrid(SendGridProperties properties) {
        return new SendGrid(properties.apiKey());
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailSender sendGridEmailSender(SendGrid sendGrid) {
        return new SendGridEmailSender(sendGrid);
    }
}
