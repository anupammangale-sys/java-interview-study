/** Temporary. The provider is down or busy. Later is genuinely worth a try. */
public class ChannelUnavailableException extends NotificationException {
    public ChannelUnavailableException(String message) { super(message); }
    @Override public boolean worthRetrying() { return true; }
}
