package sk.solodev.notify.chat.slack;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.chat.ChatAutoConfiguration;
import sk.solodev.notify.chat.ChatSender;

/**
 * Registers a Slack-backed {@link ChatSender} when {@code spring.notify.chat.slack.token}
 * is set. Runs before {@link ChatAutoConfiguration} so the sender bean exists when that
 * config's {@code @ConditionalOnBean(ChatSender.class)} adapter is evaluated.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@AutoConfiguration(before = ChatAutoConfiguration.class)
@EnableConfigurationProperties(SlackProperties.class)
@ConditionalOnProperty(prefix = "spring.notify.chat.slack", name = "token")
public class SlackChatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MethodsClient slackMethodsClient(SlackProperties properties) {
        return Slack.getInstance().methods(properties.token());
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatSender slackChatSender(MethodsClient methodsClient) {
        return new SlackChatSender(methodsClient);
    }
}
