package sk.solodev.notify.sms.vonage;

import com.vonage.client.sms.SmsClient;
import com.vonage.client.sms.SmsSubmissionResponse;
import com.vonage.client.sms.SmsSubmissionResponseMessage;
import com.vonage.client.sms.MessageStatus;
import com.vonage.client.sms.messages.TextMessage;
import sk.solodev.notify.sms.SmsRequest;
import sk.solodev.notify.sms.SmsSender;

/**
 * {@link SmsSender} backed by the Vonage SDK.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public class VonageSmsSender implements SmsSender {

    private final SmsClient smsClient;

    public VonageSmsSender(SmsClient smsClient) {
        this.smsClient = smsClient;
    }

    @Override
    public String send(SmsRequest request) {
        SmsSubmissionResponse response = smsClient.submitMessage(
                new TextMessage(request.from(), request.to(), request.message()));

        SmsSubmissionResponseMessage message = response.getMessages().getFirst();
        if (message.getStatus() != MessageStatus.OK) {
            throw new RuntimeException("Vonage rejected the message: " + message.getStatus() + " " + message.getErrorText());
        }
        return message.getId();
    }
}
