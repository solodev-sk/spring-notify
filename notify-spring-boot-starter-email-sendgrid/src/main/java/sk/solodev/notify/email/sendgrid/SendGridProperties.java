package sk.solodev.notify.email.sendgrid;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** SendGrid configuration. {@code apiKey} is the SendGrid API key. Under {@code spring.notify.email.sendgrid}. */
@ConfigurationProperties("spring.notify.email.sendgrid")
public record SendGridProperties(String apiKey) {

}
