package sk.solodev.notify.sms.vonage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Vonage configuration: {@code apiKey} and {@code apiSecret} from the Vonage dashboard. Under {@code spring.notify.sms.vonage}. */
@ConfigurationProperties("spring.notify.sms.vonage")
public record VonageSmsProperties(String apiKey, String apiSecret) {

}
