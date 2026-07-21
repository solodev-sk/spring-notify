package sk.solodev.notify.autoconfigure.event;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.event.EventPublishingNotificationInterceptor;

/**
 * Registers the interceptor that publishes {@code NotificationSent} / {@code NotificationFailed}
 * application events per send. Enabled by default; set {@code spring.notify.events.enabled=false}
 * to turn it off.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.notify.events", name = "enabled", matchIfMissing = true)
public class NotificationEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EventPublishingNotificationInterceptor eventPublishingNotificationInterceptor(
            ApplicationEventPublisher publisher) {
        return new EventPublishingNotificationInterceptor(publisher);
    }
}
