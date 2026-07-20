package sk.solodev.notify.push;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.ChannelAdapter;
import sk.solodev.notify.SenderChannelAdapter;

/** Registers the push channel adapter when a provider {@link PushSender} bean is present. */
@AutoConfiguration
public class PushAutoConfiguration {

    @Bean
    @ConditionalOnBean(PushSender.class)
    @ConditionalOnMissingBean
    public ChannelAdapter<PushRequest> pushChannelAdapter(PushSender sender) {
        return new SenderChannelAdapter<>(PushRequest.class, sender);
    }
}
