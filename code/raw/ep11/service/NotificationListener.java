/**
 * Anyone who wants to know what happened, without the service knowing who
 * they are. Adding metrics or an audit trail does not touch the service.
 */
public interface NotificationListener {
    default void sent(Notification n, int attempts) {}
    default void retrying(Notification n, int attempt, long delayMs) {}
    default void deadLettered(Notification n, String reason) {}
}
