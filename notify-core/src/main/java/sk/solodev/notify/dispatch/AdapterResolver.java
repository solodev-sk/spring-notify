package sk.solodev.notify.dispatch;

import sk.solodev.notify.NotificationRequest;

import java.util.List;

/**
 * Selects which adapter handles a request. Override to customise provider selection.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public interface AdapterResolver {

    /**
     * @param request    the request to route
     * @param candidates the available adapters
     * @param <T>        the request type
     * @return the single adapter that handles the request
     * @throws IllegalStateException if no adapter, or more than one, supports the request
     *                               (a wiring error — install exactly one provider per channel)
     */
    <T extends NotificationRequest> ChannelAdapter<T> resolve(T request, List<ChannelAdapter<?>> candidates);
}
