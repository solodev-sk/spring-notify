package sk.solodev.notify.sms;

import sk.solodev.notify.NotificationSender;

/** SPI implemented by SMS providers. Receives the full request; returns the provider message id. */
@FunctionalInterface
public interface SmsSender extends NotificationSender<SmsRequest> {

}
