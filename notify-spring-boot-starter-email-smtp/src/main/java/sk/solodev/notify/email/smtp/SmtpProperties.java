package sk.solodev.notify.email.smtp;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** SMTP connection settings. Configured under {@code spring.notify.email.smtp}. */
@ConfigurationProperties("spring.notify.email.smtp")
public record SmtpProperties(String host, int port,
                             @Nullable String username, @Nullable String password) {

    public SmtpProperties {
        port = port == 0 ? 587 : port;
    }
}
