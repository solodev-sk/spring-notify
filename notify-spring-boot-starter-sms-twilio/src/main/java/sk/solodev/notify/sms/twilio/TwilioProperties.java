package sk.solodev.notify.sms.twilio;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Twilio SMS credentials. Configured under {@code spring.notify.sms.twilio}.
 *
 * @param accountSid the Twilio account SID
 * @param authToken  the Twilio auth token
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
@ConfigurationProperties("spring.notify.sms.twilio")
public record TwilioProperties(String accountSid, String authToken) {

}
