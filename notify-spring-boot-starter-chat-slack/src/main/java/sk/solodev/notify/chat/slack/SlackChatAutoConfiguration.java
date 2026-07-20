package sk.solodev.notify.chat.slack;

import com.slack.api.Slack;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.chat.ChatSender;

/**
 * Registers a Slack-backed {@link ChatSender} when {@code spring.notify.chat.slack.token}
 * is set. The generic {@code ChatChannelAdapter} (in notify-spring-boot-chat) picks it up.
 */
@AutoConfiguration
@EnableConfigurationProperties(SlackProperties.class)
@ConditionalOnProperty(prefix = "spring.notify.chat.slack", name = "token")
public class SlackChatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ChatSender slackChatSender(SlackProperties properties) {
        return new SlackChatSender(Slack.getInstance().methods(properties.token()));
    }
}
