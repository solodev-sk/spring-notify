package sk.solodev.notify.autoconfigure;

import sk.solodev.notify.dispatch.AdapterResolver;
import sk.solodev.notify.dispatch.ChannelAdapter;
import sk.solodev.notify.dispatch.DefaultAdapterResolver;
import sk.solodev.notify.dispatch.DefaultNotifier;
import sk.solodev.notify.interceptor.NotificationInterceptor;
import sk.solodev.notify.Notifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

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
     * @param interceptors the interceptor beans; {@link DefaultNotifier} sorts them by
     *                     {@code @Order}/{@link org.springframework.core.Ordered}
     * @param taskExecutor Boot's {@code applicationTaskExecutor} for {@code notifyAsync}; falls
     *                     back to the common {@link ForkJoinPool} if none is present
     * @return the notifier consumers inject
     */
    @Bean
    @ConditionalOnMissingBean
    public Notifier notifier(List<ChannelAdapter<?>> adapters, AdapterResolver resolver,
                             List<NotificationInterceptor> interceptors,
                             @Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
                             ObjectProvider<Executor> taskExecutor) {
        Executor executor = taskExecutor.getIfAvailable(ForkJoinPool::commonPool);
        return new DefaultNotifier(adapters, resolver, interceptors, executor);
    }
}
