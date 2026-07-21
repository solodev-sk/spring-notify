package sk.solodev.notify.chat.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Slack configuration. Configured under {@code spring.notify.chat.slack}.
 *
 * @param token the Slack bot token (xoxb-…)
 */
@ConfigurationProperties("spring.notify.chat.slack")
public record SlackProperties(String token) {

}
