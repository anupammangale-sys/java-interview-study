/** Permanent. The address is wrong and will still be wrong in ten minutes. */
public class InvalidRecipientException extends NotificationException {
    public InvalidRecipientException(String message) { super(message); }
    @Override public boolean worthRetrying() { return false; }
}
