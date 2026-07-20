package sk.solodev.notify.email;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.ChannelAdapter;
import sk.solodev.notify.SenderChannelAdapter;

/** Registers the email channel adapter when a provider {@link EmailSender} bean is present. */
@AutoConfiguration
public class EmailAutoConfiguration {

    @Bean
    @ConditionalOnBean(EmailSender.class)
    @ConditionalOnMissingBean
    public ChannelAdapter<EmailRequest> emailChannelAdapter(EmailSender sender) {
        return new SenderChannelAdapter<>(EmailRequest.class, sender);
    }
}
