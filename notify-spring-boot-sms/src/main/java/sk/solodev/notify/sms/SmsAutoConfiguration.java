package sk.solodev.notify.sms;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.ChannelAdapter;
import sk.solodev.notify.SenderChannelAdapter;

/** Registers the SMS channel adapter when a provider {@link SmsSender} bean is present. */
@AutoConfiguration
public class SmsAutoConfiguration {

    @Bean
    @ConditionalOnBean(SmsSender.class)
    @ConditionalOnMissingBean
    public ChannelAdapter<SmsRequest> smsChannelAdapter(SmsSender sender) {
        return new SenderChannelAdapter<>(SmsRequest.class, sender);
    }
}
