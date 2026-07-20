package sk.solodev.notify.sms.twilio;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Twilio SMS credentials. Configured under {@code spring.notify.sms.twilio}. */
@ConfigurationProperties("spring.notify.sms.twilio")
public record TwilioSmsProperties(String accountSid, String authToken) {

}
