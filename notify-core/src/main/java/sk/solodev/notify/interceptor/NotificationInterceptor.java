package sk.solodev.notify.interceptor;

import sk.solodev.notify.NotificationRequest;

/**
 * Cross-cutting hook around each send. Interceptors are ordered (Spring
 * {@code @Order}/{@code Ordered}). An interceptor may inspect or replace the
 * request, short-circuit by returning a message id without calling the chain, or
 * inspect the resulting id after calling it.
 */
@FunctionalInterface
public interface NotificationInterceptor {

    /**
     * @param request the request being sent
     * @param chain   the rest of the pipeline; call {@link Chain#proceed} to continue
     * @return the provider message id
     */
    String intercept(NotificationRequest request, Chain chain);

    /** The continuation of the interceptor pipeline from one interceptor to the next. */
    @FunctionalInterface
    interface Chain {

        /**
         * Continue the pipeline.
         *
         * @param request the request to pass on
         * @return the provider message id from the remaining chain
         */
        String proceed(NotificationRequest request);
    }
}
