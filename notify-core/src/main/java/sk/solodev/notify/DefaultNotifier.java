package sk.solodev.notify;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Default {@link Notifier}: resolves the adapter for a request via the {@link AdapterResolver}
 * and invokes it through the ordered {@link NotificationInterceptor} chain.
 */
public class DefaultNotifier implements Notifier {

    private final List<ChannelAdapter<?>> adapters;

    private final AdapterResolver resolver;

    private final List<NotificationInterceptor> interceptors;

    private final Executor executor;

    /**
     * Creates a notifier whose {@link #notifyAsync(NotificationRequest)} runs on the common
     * {@link ForkJoinPool}.
     *
     * @param adapters     the available channel adapters
     * @param resolver     selects the adapter for a given request
     * @param interceptors interceptors to apply around each send, <strong>in the order they
     *                     should run</strong> (outermost first) — this class does not sort them;
     *                     the auto-configuration orders them by {@code @Order}/{@code Ordered}
     */
    public DefaultNotifier(List<ChannelAdapter<?>> adapters, AdapterResolver resolver,
                           List<NotificationInterceptor> interceptors) {
        this(adapters, resolver, interceptors, ForkJoinPool.commonPool());
    }

    /**
     * @param adapters     the available channel adapters
     * @param resolver     selects the adapter for a given request
     * @param interceptors interceptors to apply around each send, in the order they should run
     *                     (outermost first); this class does not sort them
     * @param executor     runs {@link #notifyAsync(NotificationRequest)}; typically the
     *                     application task executor (honours virtual threads when enabled)
     */
    public DefaultNotifier(List<ChannelAdapter<?>> adapters, AdapterResolver resolver,
                           List<NotificationInterceptor> interceptors, Executor executor) {
        this.adapters = List.copyOf(adapters);
        this.resolver = resolver;
        this.interceptors = List.copyOf(interceptors);
        this.executor = executor;
    }

    @Override
    public String notify(NotificationRequest request) {
        return chainFrom(0).proceed(request);
    }

    @Override
    public CompletableFuture<String> notifyAsync(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> notify(request), executor);
    }

    private NotificationInterceptor.Chain chainFrom(int index) {
        if (index < interceptors.size()) {
            return request -> interceptors.get(index).intercept(request, chainFrom(index + 1));
        }
        return request -> resolver.resolve(request, adapters).deliver(request);
    }
}
