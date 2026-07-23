package sk.solodev.notify.outbox;

/**
 * Lifecycle state of an outbox entry.
 *
 * @author Dominik Kovács
 * @since 1.0.1
 */
public enum OutboxStatus {

    /** Awaiting delivery (or a retry). */
    PENDING,

    /** Accepted by the provider. */
    SENT,

    /** Delivery abandoned after exhausting attempts. */
    FAILED
}