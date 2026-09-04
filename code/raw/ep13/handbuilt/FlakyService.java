import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stands in for a downstream service. It counts every call that actually
 * reaches it, which is the number the whole episode is about: the point of
 * these patterns is to reduce it when the service is unwell.
 */
public class FlakyService {

    private final AtomicInteger callsReceived = new AtomicInteger();
    private volatile boolean healthy = true;
    private volatile long latencyMs = 5;

    public String call(String what) {
        callsReceived.incrementAndGet();
        sleep(latencyMs);
        if (!healthy) {
            throw new RuntimeException("downstream is down");
        }
        return "ok:" + what;
    }

    public void goDown()      { healthy = false; }
    public void comeBack()    { healthy = true; }
    public void slowTo(long ms) { latencyMs = ms; }

    public int callsReceived() { return callsReceived.get(); }
    public void resetCounter() { callsReceived.set(0); }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
