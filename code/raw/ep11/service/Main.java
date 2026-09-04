import java.util.List;

/**
 * Runs the notification service end to end.
 *
 *   java Main.java
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Channels channels = new Channels(List.of(
                new EmailChannel(), new SmsChannel(), new PushChannel()));
        DeadLetterQueue dlq = new DeadLetterQueue();
        MetricsListener metrics = new MetricsListener();

        try (NotificationService service = new NotificationService(
                channels, dlq, List.of(metrics, new AuditListener()),
                /* queue capacity */ 10, /* workers */ 3,
                /* max attempts */ 3, /* base delay ms */ 50)) {

            System.out.println("=== 1. three channels, one interface ===");
            service.submit(new Notification("N-1", "asha@example.com", "email", "Order shipped", null));
            service.submit(new Notification("N-2", "+919876543210", "sms", "OTP 4417", null));
            service.submit(new Notification("N-3", "device-77", "push", "You have a reply", null));
            service.drain(3000);

            System.out.println();
            System.out.println("=== 2. a permanent failure and a temporary one ===");
            System.out.println("  bad address, cannot ever work:");
            service.submit(new Notification("N-4", "asha-at-example.com", "email", "Order shipped", null));
            service.drain(3000);
            System.out.println("  provider busy, worth trying again:");
            service.submit(new Notification("N-5", "+919000000001", "sms", "OTP 8823", null));
            service.drain(3000);

            System.out.println();
            System.out.println("=== 3. the same request arrives twice ===");
            service.submit(new Notification("N-6", "ravi@example.com", "email", "Payment received", "pay-9912"));
            service.submit(new Notification("N-7", "ravi@example.com", "email", "Payment received", "pay-9912"));
            service.drain(3000);

            System.out.println();
            System.out.println("=== 4. more work than the queue can hold ===");
            System.out.println("  queue capacity is 10, submitting 40 at once:");
            int accepted = 0;
            for (int i = 0; i < 40; i++) {
                if (service.submit(new Notification("B-" + i, "device-" + i, "push", "burst", null))) {
                    accepted++;
                }
            }
            System.out.println("  accepted " + accepted + ", rejected " + service.rejected());
            service.drain(5000);

            System.out.println();
            System.out.println("=== what happened ===");
            System.out.println("  " + metrics.summary());
            System.out.println("  duplicates ignored: " + service.duplicatesIgnored());
            System.out.println("  dead letter queue holds " + dlq.size() + ":");
            for (DeadLetterQueue.Entry e : dlq.all()) {
                System.out.println("    " + e.notification() + "  after " + e.attempts()
                        + " attempt(s), " + e.reason());
            }
        }
    }
}
