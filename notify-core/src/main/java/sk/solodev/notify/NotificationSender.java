package sk.solodev.notify;

import sk.solodev.notify.dispatch.ChannelAdapter;
import sk.solodev.notify.dispatch.SenderChannelAdapter;

/**
 * SPI implemented by a provider for one channel: takes the canonical request and performs
 * the actual delivery, returning the provider message id. Each channel declares its own
 * sub-interface (e.g. {@code SmsSender extends NotificationSender<SmsRequest>}) so provider
 * beans can be selected by type. The generic {@link SenderChannelAdapter} adapts any sender
 * into a {@link ChannelAdapter}.
 *
 * @param <T> the channel request type this sender delivers
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@FunctionalInterface
public interface NotificationSender<T extends NotificationRequest> {

    /**
     * Perform the delivery and return the provider message id. May throw any exception
     * (including checked provider-SDK exceptions); the {@link SenderChannelAdapter}
     * normalises it to a {@link NotificationDeliveryException}.
     *
     * @param request the request to deliver
     * @return the provider message id
     * @throws Exception if the provider call fails
     */
    String send(T request) throws Exception;
}
