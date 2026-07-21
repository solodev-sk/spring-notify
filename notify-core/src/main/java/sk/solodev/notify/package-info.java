/**
 * Core of the spring-notify framework: the channel-agnostic delivery pipeline and its contracts.
 *
 * <p>Consumers call {@link sk.solodev.notify.Notifier} with a {@link sk.solodev.notify.NotificationRequest};
 * the {@link sk.solodev.notify.dispatch.AdapterResolver} selects the matching
 * {@link sk.solodev.notify.dispatch.ChannelAdapter}, which is invoked through the
 * {@link sk.solodev.notify.interceptor.NotificationInterceptor} chain. Providers implement the
 * {@link sk.solodev.notify.NotificationSender} SPI; the generic
 * {@link sk.solodev.notify.dispatch.SenderChannelAdapter} adapts a sender into a channel adapter.
 *
 * <p>All types in this package are non-null by default ({@link org.jspecify.annotations.NullMarked}).
 */
@NullMarked
package sk.solodev.notify;

import org.jspecify.annotations.NullMarked;
