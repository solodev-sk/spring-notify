package sk.solodev.notify.sms.twilio;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Twilio SMS credentials. Configured under {@code spring.notify.sms.twilio}.
 *
 * @param accountSid the Twilio account SID
 * @param authToken  the Twilio auth token
 */
@ConfigurationProperties("spring.notify.sms.twilio")
public record TwilioSmsProperties(String accountSid, String authToken) {

}
