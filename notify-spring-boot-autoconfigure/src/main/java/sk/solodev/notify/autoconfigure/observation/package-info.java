/**
 * Micrometer observability for notification sends — an interceptor that records one
 * observation (timer + tracing span + logs) per send, with a customizable convention.
 *
 * <p>All types in this package are non-null by default ({@link org.jspecify.annotations.NullMarked}).
 */
@NullMarked
package sk.solodev.notify.autoconfigure.observation;

import org.jspecify.annotations.NullMarked;
