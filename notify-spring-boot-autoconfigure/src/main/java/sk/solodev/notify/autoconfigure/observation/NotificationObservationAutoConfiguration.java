package sk.solodev.notify.autoconfigure.observation;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers Micrometer observability for notifications when {@link ObservationRegistry} is on
 * the classpath and a registry bean exists (e.g. with Spring Boot Actuator). Contributes one
 * observation per send — a timer, a tracing span, and structured logs. Absent Micrometer, none
 * of this loads and the framework has no observability dependency.
 */
@AutoConfiguration
@ConditionalOnClass(ObservationRegistry.class)
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
