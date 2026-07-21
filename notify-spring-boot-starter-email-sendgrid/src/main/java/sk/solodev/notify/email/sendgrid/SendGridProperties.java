package sk.solodev.notify.email.sendgrid;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SendGrid configuration. Configured under {@code spring.notify.email.sendgrid}.
 *
 * @param apiKey the SendGrid API key
 */
@ConfigurationProperties("spring.notify.email.sendgrid")
public record SendGridProperties(String apiKey) {

}
