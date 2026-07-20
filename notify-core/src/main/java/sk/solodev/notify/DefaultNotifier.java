package sk.solodev.notify;

import java.util.List;

/**
 * Default {@link Notifier}: resolves the adapter for a request via the {@link AdapterResolver}
 * and invokes it through the ordered {@link NotificationInterceptor} chain.
 */
public class DefaultNotifier implements Notifier {

    private final List<ChannelAdapter<?>> adapters;

    private final AdapterResolver resolver;

    private final List<NotificationInterceptor> interceptors;

    /**
     * @param adapters     the available channel adapters
     * @param resolver     selects the adapter for a given request
     * @param interceptors interceptors to apply around each send, <strong>in the order they
     *                     should run</strong> (outermost first) — this class does not sort them;
     *                     the autoconfiguration orders them by {@code @Order}/{@link org.springframework.core.Ordered}
     */
    public DefaultNotifier(List<ChannelAdapter<?>> adapters, AdapterResolver resolver,
                           List<NotificationInterceptor> interceptors) {
        this.adapters = List.copyOf(adapters);
        this.resolver = resolver;
        this.interceptors = List.copyOf(interceptors);
    }

    @Override
    public String notify(NotificationRequest request) {
        return chainFrom(0).proceed(request);
    }

    private NotificationInterceptor.Chain chainFrom(int index) {
        if (index < interceptors.size()) {
            return request -> interceptors.get(index).intercept(request, chainFrom(index + 1));
        }
        return request -> resolver.resolve(request, adapters).deliver(request);
    }
}
