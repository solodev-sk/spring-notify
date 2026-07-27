package sk.solodev.notify.outbox;

import sk.solodev.notify.NotificationRequest;

import java.util.UUID;

/**
 * Durable, transactionally-consistent sending. {@link #enqueue} persists the request in the
 * caller's current transaction; a relay delivers it later through the normal {@code Notifier}
 * pipeline. Because delivery is asynchronous, no provider message id exists yet — the returned
 * {@link UUID} is the outbox entry id, which correlates with the eventual
 * {@code NotificationSent}/{@code NotificationFailed} event.
 *
 * @author Dominik Kovács
 * @since 1.1.0
 */
public interface OutboxNotifier {

    /**
     * Persist {@code request} for durable delivery, joining the active transaction.
     *
     * @param request the request to deliver later
     * @return the outbox entry id (not a provider message id)
     */
    UUID enqueue(NotificationRequest request);
}