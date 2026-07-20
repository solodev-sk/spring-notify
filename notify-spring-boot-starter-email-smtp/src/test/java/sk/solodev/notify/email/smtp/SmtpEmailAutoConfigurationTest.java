package sk.solodev.notify.email.smtp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import sk.solodev.notify.ChannelAdapter;
import sk.solodev.notify.email.EmailAutoConfiguration;
import sk.solodev.notify.email.EmailSender;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpEmailAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SmtpEmailAutoConfiguration.class));

    @Test
    void registersMailSenderAndEmailSenderFromNotifyProperties() {
        runner.withPropertyValues(
                "spring.notify.email.smtp.host=smtp.example.com",
                "spring.notify.email.smtp.port=2525",
                "spring.notify.email.smtp.username=apikey",
                "spring.notify.email.smtp.password=secret").run(ctx -> {
            assertThat(ctx).hasSingleBean(EmailSender.class);
            assertThat(ctx).getBean(EmailSender.class).isInstanceOf(SmtpEmailSender.class);

            var mailSender = (JavaMailSenderImpl) ctx.getBean(JavaMailSender.class);
            assertThat(mailSender.getHost()).isEqualTo("smtp.example.com");
            assertThat(mailSender.getPort()).isEqualTo(2525);
            assertThat(mailSender.getUsername()).isEqualTo("apikey");
        });
    }

    @Test
    void defaultsPortTo587WhenUnset() {
        runner.withPropertyValues("spring.notify.email.smtp.host=smtp.example.com").run(ctx -> {
            var mailSender = (JavaMailSenderImpl) ctx.getBean(JavaMailSender.class);
            assertThat(mailSender.getPort()).isEqualTo(587);
        });
    }

    @Test
    void noBeansWhenHostAbsent() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(EmailSender.class));
    }

    /**
     * The channel adapter is gated by {@code @ConditionalOnBean(EmailSender.class)}, so this
     * SMTP config must be ordered before {@link EmailAutoConfiguration}. Loading both (channel
     * first) proves the ordering makes the adapter register.
     */
    @Test
    void channelAdapterRegistersWhenBothConfigsPresent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        EmailAutoConfiguration.class, SmtpEmailAutoConfiguration.class))
                .withPropertyValues("spring.notify.email.smtp.host=smtp.example.com")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(EmailSender.class);
                    assertThat(ctx).hasSingleBean(ChannelAdapter.class);
                });
    }
}
