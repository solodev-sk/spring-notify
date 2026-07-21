package sk.solodev.notify.email.smtp;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import sk.solodev.notify.email.EmailAutoConfiguration;
import sk.solodev.notify.email.EmailSender;

/**
 * Registers an SMTP-backed {@link EmailSender} when {@code spring.notify.email.smtp.host}
 * is set. Builds its own {@link JavaMailSender} from {@code spring.notify.email.smtp.*}
 * so configuration matches the other channels (rather than Spring Boot's {@code spring.mail.*}).
 * Runs before {@link EmailAutoConfiguration} so the sender bean exists when that config's
 * {@code @ConditionalOnBean(EmailSender.class)} adapter is evaluated.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@AutoConfiguration(before = EmailAutoConfiguration.class)
@EnableConfigurationProperties(SmtpProperties.class)
@ConditionalOnProperty(prefix = "spring.notify.email.smtp", name = "host")
public class SmtpEmailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JavaMailSender notifyMailSender(SmtpProperties properties) {
        var sender = new JavaMailSenderImpl();
        sender.setHost(properties.host());
        sender.setPort(properties.port());
        if (properties.username() != null) {
            sender.setUsername(properties.username());
        }
        if (properties.password() != null) {
            sender.setPassword(properties.password());
        }
        return sender;
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailSender smtpEmailSender(JavaMailSender mailSender) {
        return new SmtpEmailSender(mailSender);
    }
}
