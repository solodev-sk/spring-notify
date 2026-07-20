package sk.solodev.notify.chat.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Slack configuration. {@code token} is a bot token (xoxb-…). Configured under {@code spring.notify.chat.slack}. */
@ConfigurationProperties("spring.notify.chat.slack")
public record SlackProperties(String token) {

}
