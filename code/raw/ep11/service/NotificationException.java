/**
 * The base of the exception tree. The important field is not the message,
 * it is whether trying again could ever work. Every retry decision in the
 * whole system reads this one method, so the decision lives in one place
 * instead of in a switch somewhere.
 */
public abstract class NotificationException extends Exception {
    protected NotificationException(String message) { super(message); }
    public abstract boolean worthRetrying();
}
