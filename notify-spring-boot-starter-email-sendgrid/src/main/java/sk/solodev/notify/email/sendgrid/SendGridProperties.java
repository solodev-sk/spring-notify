package sk.solodev.notify.email.sendgrid;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SendGrid configuration. Configured under {@code spring.notify.email.sendgrid}.
 *
 * @param apiKey the SendGrid API key
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@ConfigurationProperties("spring.notify.email.sendgrid")
public record SendGridProperties(String apiKey) {

}
