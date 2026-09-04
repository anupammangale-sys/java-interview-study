/**
 * One way of getting a notification to a person. Adding a fourth channel is a
 * new file that implements this, and no existing file changes.
 */
public interface Channel {
    String name();
    void send(Notification n) throws NotificationException;
}
