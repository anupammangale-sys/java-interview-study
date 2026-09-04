/**
 * One thing to send. A record because it never changes after it is created,
 * which matters once several worker threads are holding the same one.
 */
public record Notification(String id, String recipient, String channel,
                           String body, String idempotencyKey) {

    public Notification {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipient is required");
        }
    }

    @Override public String toString() { return id + " to " + recipient + " by " + channel; }
}
