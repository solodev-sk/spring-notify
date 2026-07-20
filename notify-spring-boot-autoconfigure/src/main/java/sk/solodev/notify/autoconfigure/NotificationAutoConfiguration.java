package sk.solodev.notify.autoconfigure;

import sk.solodev.notify.AdapterResolver;
import sk.solodev.notify.ChannelAdapter;
import sk.solodev.notify.DefaultAdapterResolver;
import sk.solodev.notify.DefaultNotifier;
import sk.solodev.notify.NotificationInterceptor;
import sk.solodev.notify.Notifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-configures the notification pipeline: a {@link DefaultAdapterResolver} and a
 * {@link DefaultNotifier} wired with all {@link ChannelAdapter} and
 * {@link NotificationInterceptor} beans. Both back off if the application defines its own.
 */
@AutoConfiguration
public class NotificationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AdapterResolver adapterResolver() {
        return new DefaultAdapterResolver();
    }

    /**
     * @param adapters     the channel adapter beans on the context
     * @param resolver     the adapter resolver
     * @param interceptors the interceptor beans, sorted here by {@code @Order}/{@link org.springframework.core.Ordered}
     * @return the notifier consumers inject
     */
    @Bean
    @ConditionalOnMissingBean
    public Notifier notifier(List<ChannelAdapter<?>> adapters, AdapterResolver resolver,
                             List<NotificationInterceptor> interceptors) {
        var ordered = new ArrayList<>(interceptors);
        ordered.sort(AnnotationAwareOrderComparator.INSTANCE);
        return new DefaultNotifier(adapters, resolver, ordered);
    }
}
