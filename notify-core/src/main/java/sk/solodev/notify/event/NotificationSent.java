package sk.solodev.notify.event;

import sk.solodev.notify.NotificationRequest;

/**
 * Published after a notification is accepted by its provider.
 *
 * @param request   the request that was sent
 * @param messageId the provider message id returned by the delivery
 */
public record NotificationSent(NotificationRequest request, String messageId) implements NotificationEvent {

}
