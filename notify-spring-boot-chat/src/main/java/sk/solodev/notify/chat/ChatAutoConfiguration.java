package sk.solodev.notify.chat;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.dispatch.ChannelAdapter;
import sk.solodev.notify.dispatch.SenderChannelAdapter;

/** Registers the chat channel adapter when a provider {@link ChatSender} bean is present. */
@AutoConfiguration
public class ChatAutoConfiguration {

    @Bean
    @ConditionalOnBean(ChatSender.class)
    @ConditionalOnMissingBean
    public ChannelAdapter<ChatRequest> chatChannelAdapter(ChatSender sender) {
        return new SenderChannelAdapter<>(ChatRequest.class, sender);
    }
}
