package sk.solodev.notify.email;

import sk.solodev.notify.NotificationSender;

/** SPI implemented by email providers. Receives the full request; returns the provider message id. */
@FunctionalInterface
public interface EmailSender extends NotificationSender<EmailRequest> {
}
