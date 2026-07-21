package sk.solodev.notify.push;

import sk.solodev.notify.NotificationSender;

/**
 * SPI implemented by push providers. Receives the full request; returns the provider message id.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@FunctionalInterface
public interface PushSender extends NotificationSender<PushRequest> {

}
