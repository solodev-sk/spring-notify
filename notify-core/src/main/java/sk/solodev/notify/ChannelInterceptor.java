package sk.solodev.notify;

/**
 * A {@link NotificationInterceptor} scoped to one channel: it only runs its logic for
 * requests of type {@code T} and transparently passes any other request down the chain.
 * Subclass and implement {@link #interceptForChannel} to intercept a single channel
 * without a per-implementation {@code instanceof} guard.
 */
public abstract class ChannelInterceptor<T extends NotificationRequest> implements NotificationInterceptor {

    private final Class<T> requestType;

    protected ChannelInterceptor(Class<T> requestType) {
        this.requestType = requestType;
    }

    @Override
    public final String intercept(NotificationRequest request, Chain chain) {
        return requestType.isInstance(request)
                ? interceptForChannel(requestType.cast(request), chain)
                : chain.proceed(request);
    }

    /** Invoked only for requests of type {@code T}. */
    protected abstract String interceptForChannel(T request, Chain chain);
}
