package sk.solodev.notify.autoconfigure.observation;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import sk.solodev.notify.DefaultNotificationObservationConvention;
import sk.solodev.notify.NotificationObservationConvention;
import sk.solodev.notify.ObservationNotificationInterceptor;

/**
 * Registers Micrometer observability for notifications when an {@link ObservationRegistry} bean
 * exists — contributed by Boot's {@code spring-boot-micrometer-observation} module (pulled in by
 * {@code spring-boot-starter-actuator}, among others). Adds one observation per send — a timer, a
 * tracing span, and structured logs. Without a registry bean, nothing is registered and sends are
 * unobserved.
 */
@AutoConfiguration
@ConditionalOnBean(ObservationRegistry.class)
public class NotificationObservationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(NotificationObservationConvention.class)
    public NotificationObservationConvention notificationObservationConvention() {
        return new DefaultNotificationObservationConvention();
    }

    @Bean
    @ConditionalOnMissingBean
    public ObservationNotificationInterceptor observationNotificationInterceptor(
            ObservationRegistry registry, NotificationObservationConvention convention) {
        return new ObservationNotificationInterceptor(registry, convention);
    }
}
