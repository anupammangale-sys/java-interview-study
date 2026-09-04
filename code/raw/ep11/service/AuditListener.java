public class AuditListener implements NotificationListener {
    public void sent(Notification n, int attempts) {
        System.out.println("      [audit] " + n.id() + " sent after " + attempts + " attempt(s)");
    }
    public void deadLettered(Notification n, String reason) {
        System.out.println("      [audit] " + n.id() + " GAVE UP: " + reason);
    }
}
