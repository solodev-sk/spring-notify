package sk.solodev.notify.dispatch;

import sk.solodev.notify.NotificationRequest;

import java.util.List;

/** Picks the single adapter whose {@code supports} returns true. */
public class DefaultAdapterResolver implements AdapterResolver {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends NotificationRequest> ChannelAdapter<T> resolve(T request, List<ChannelAdapter<?>> candidates) {
        var matches = candidates.stream().filter(a -> a.supports(request)).toList();
        if (matches.isEmpty()) {
            throw new IllegalStateException(
                    "No ChannelAdapter supports request type " + request.getClass().getName());
        }
        if (matches.size() > 1) {
            throw new IllegalStateException(
                    matches.size() + " ChannelAdapters support request type "
                            + request.getClass().getName()
                            + "; install one provider per channel");
        }
        return (ChannelAdapter<T>) matches.getFirst();
    }
}
