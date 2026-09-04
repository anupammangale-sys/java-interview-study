import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fails the first two attempts for any recipient, then succeeds. Real
 * providers fail unpredictably; this one fails predictably so the numbers
 * in the output can be trusted.
 */
public class SmsChannel implements Channel {
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();

    public String name() { return "sms"; }

    public void send(Notification n) throws NotificationException {
        int attempt = attempts.merge(n.recipient(), 1, Integer::sum);
        if (attempt <= 2) {
            throw new ChannelUnavailableException(
                    "sms provider busy, attempt " + attempt + " for " + n.recipient());
        }
        System.out.println("      [sms] delivered to " + n.recipient() + " on attempt " + attempt);
    }
}
