package sk.solodev.notify.sms.twilio;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import sk.solodev.notify.sms.SmsRequest;
import sk.solodev.notify.sms.SmsSender;

/** {@link SmsSender} backed by the Twilio SDK. */
public class TwilioSmsSender implements SmsSender {

    private final TwilioRestClient restClient;

    public TwilioSmsSender(TwilioRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String send(SmsRequest request) {
        Message message = Message.creator(
                new PhoneNumber(request.to()),
                new PhoneNumber(request.from()),
                request.message()).create(restClient);
        return message.getSid();
    }
}
