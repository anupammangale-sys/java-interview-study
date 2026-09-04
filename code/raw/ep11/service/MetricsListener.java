import java.util.concurrent.atomic.AtomicInteger;

public class MetricsListener implements NotificationListener {
    final AtomicInteger sent = new AtomicInteger();
    final AtomicInteger retries = new AtomicInteger();
    final AtomicInteger dead = new AtomicInteger();
    final AtomicInteger attemptsTotal = new AtomicInteger();

    public void sent(Notification n, int attempts) {
        sent.incrementAndGet(); attemptsTotal.addAndGet(attempts);
    }
    public void retrying(Notification n, int attempt, long delayMs) { retries.incrementAndGet(); }
    public void deadLettered(Notification n, String reason) { dead.incrementAndGet(); }

    public String summary() {
        return "sent " + sent + ", dead lettered " + dead
             + ", retries " + retries + ", attempts used " + attemptsTotal;
    }
}
