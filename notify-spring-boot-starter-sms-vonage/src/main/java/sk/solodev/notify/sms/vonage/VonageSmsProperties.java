package sk.solodev.notify.sms.vonage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Vonage credentials from the Vonage dashboard. Configured under {@code spring.notify.sms.vonage}.
 *
 * @param apiKey    the Vonage API key
 * @param apiSecret the Vonage API secret
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@ConfigurationProperties("spring.notify.sms.vonage")
public record VonageSmsProperties(String apiKey, String apiSecret) {

}
