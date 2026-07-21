package sk.solodev.notify;

import sk.solodev.notify.dispatch.ChannelAdapter;
import sk.solodev.notify.dispatch.DefaultNotifier;
import sk.solodev.notify.interceptor.NotificationInterceptor;

import java.util.concurrent.CompletableFuture;

/**
 * Entry point for sending notifications. Consumers inject a {@code Notifier} and call
 * {@link #notify(NotificationRequest)} with a channel request (an {@code SmsRequest},
 * {@code EmailRequest}, …); the notifier routes it to the matching {@link ChannelAdapter},
 * applying any registered {@link NotificationInterceptor}s around the delivery.
 */
public interface Notifier {

    /**
     * Deliver a notification.
     *
     * @param request the channel request to send; its concrete type selects the channel
     * @return the provider message id of the accepted delivery
     * @throws IllegalStateException if no adapter, or more than one, handles the request type
     * @throws NotificationDeliveryException if the provider fails to deliver
     */
    String notify(NotificationRequest request);

    /**
     * Deliver a notification asynchronously. Runs the full pipeline — interceptors, adapter
     * resolution, and the provider call — off the calling thread, so a blocking send (SMTP,
     * an HTTP call to a provider) never stalls the caller.
     *
     * <p>The returned future completes with the provider message id, or completes exceptionally
     * with the same failures {@link #notify(NotificationRequest)} would throw
     * ({@link NotificationDeliveryException}, {@link IllegalStateException}).
     *
     * <p>The default implementation runs on the common {@link java.util.concurrent.ForkJoinPool};
     * {@link DefaultNotifier} runs it on the executor it was configured with (the application
     * task executor, which honours virtual threads when enabled).
     *
     * @param request the channel request to send; its concrete type selects the channel
     * @return a future for the provider message id of the accepted delivery
     */
    default CompletableFuture<String> notifyAsync(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> notify(request));
    }
}
