package sk.solodev.notify.chat;

import sk.solodev.notify.NotificationSender;

/**
 * SPI implemented by chat providers. Receives the full request; returns the provider message id.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@FunctionalInterface
public interface ChatSender extends NotificationSender<ChatRequest> {

}
