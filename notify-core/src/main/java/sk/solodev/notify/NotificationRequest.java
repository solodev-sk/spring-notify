package sk.solodev.notify;

/**
 * Marker for a self-contained, provider-agnostic request to deliver one notification
 * over a channel. The concrete type is the channel discriminator (an {@code SmsRequest}
 * lives in the SMS channel module, a {@code ChatRequest} in the chat module) — that is
 * what the resolver routes on. Each request type carries its own portable fields plus,
 * where useful, an {@code attributes} map for provider-specific extras.
 */
public interface NotificationRequest {

}
