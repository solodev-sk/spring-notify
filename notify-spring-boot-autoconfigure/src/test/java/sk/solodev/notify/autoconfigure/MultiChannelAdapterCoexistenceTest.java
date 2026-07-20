package sk.solodev.notify.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.solodev.notify.ChannelAdapter;
import sk.solodev.notify.NotificationRequest;
import sk.solodev.notify.SenderChannelAdapter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two channel modules each register a {@code ChannelAdapter} bean guarded by
 * {@code @ConditionalOnMissingBean}. This proves the generic parameter keeps them
 * distinct — the condition must not collapse different-typed adapters into one.
 */
class MultiChannelAdapterCoexistenceTest {

    private record RequestA() implements NotificationRequest {
    }

    private record RequestB() implements NotificationRequest {
    }

    @Configuration
    static class TwoChannels {

        @Bean
        @ConditionalOnMissingBean
        ChannelAdapter<RequestA> adapterA() {
            return new SenderChannelAdapter<>(RequestA.class, r -> "a");
        }

        @Bean
        @ConditionalOnMissingBean
        ChannelAdapter<RequestB> adapterB() {
            return new SenderChannelAdapter<>(RequestB.class, r -> "b");
        }
    }

    @Test
    void differentlyTypedAdaptersCoexist() {
        new ApplicationContextRunner()
                .withUserConfiguration(TwoChannels.class)
                .run(ctx -> assertThat(ctx.getBeansOfType(ChannelAdapter.class)).hasSize(2));
    }
}
