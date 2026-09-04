import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The service the whole round is built around. It owns four decisions and
 * nothing else: what goes on the queue, who picks it up, what happens when
 * something fails, and who gets told.
 */
public class NotificationService implements AutoCloseable {

    /** One item of work, carrying how many times it has already been tried. */
    private record Job(Notification n, int attempt) {}

    private final Channels channels;
    private final DeadLetterQueue dlq;
    private final List<NotificationListener> listeners;
    private final int maxAttempts;
    private final long baseDelayMs;

    private final BlockingQueue<Job> queue;
    private final List<Thread> workers = new ArrayList<>();
    private final ScheduledExecutorService retryClock =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "retry-clock"); t.setDaemon(true); return t;
            });

    private final Map<String, String> alreadyHandled = new ConcurrentHashMap<>();
    private final AtomicInteger rejected = new AtomicInteger();
    private final AtomicInteger duplicatesIgnored = new AtomicInteger();
    private final AtomicInteger inFlight = new AtomicInteger();
    private volatile boolean running = true;

    public NotificationService(Channels channels, DeadLetterQueue dlq,
                               List<NotificationListener> listeners,
                               int queueCapacity, int workerCount,
                               int maxAttempts, long baseDelayMs) {
        this.channels = channels;
        this.dlq = dlq;
        this.listeners = List.copyOf(listeners);
        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);

        for (int i = 0; i < workerCount; i++) {
            Thread t = new Thread(this::workerLoop, "worker-" + (i + 1));
            t.setDaemon(true);
            workers.add(t);
            t.start();
        }
    }

    /**
     * Hand a notification in. Returns false when the queue is full, which is
     * the honest answer: the caller decides whether to wait, drop it or fail.
     * Blocking here instead would push the problem up the stack invisibly.
     */
    public boolean submit(Notification n) {
        String key = n.idempotencyKey();
        if (key != null && alreadyHandled.putIfAbsent(key, n.id()) != null) {
            duplicatesIgnored.incrementAndGet();
            System.out.println("    ignored duplicate of " + n.id() + ", key " + key);
            return true;
        }
        inFlight.incrementAndGet();
        boolean accepted = queue.offer(new Job(n, 1));
        if (!accepted) {
            inFlight.decrementAndGet();
            rejected.incrementAndGet();
            System.out.println("    REJECTED " + n.id() + ", queue is full");
        }
        return accepted;
    }

    private void workerLoop() {
        while (running) {
            Job job;
            try {
                job = queue.poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (job != null) handle(job);
        }
    }

    private void handle(Job job) {
        Notification n = job.n();
        try {
            channels.get(n.channel()).send(n);
            listeners.forEach(l -> l.sent(n, job.attempt()));
            inFlight.decrementAndGet();
        } catch (NotificationException e) {
            // the whole retry decision is this one line
            if (!e.worthRetrying()) {
                giveUp(n, "permanent: " + e.getMessage(), job.attempt());
            } else if (job.attempt() >= maxAttempts) {
                giveUp(n, "gave up after " + maxAttempts + " attempts: " + e.getMessage(),
                        job.attempt());
            } else {
                long delay = baseDelayMs * (1L << (job.attempt() - 1));   // 1x, 2x, 4x
                listeners.forEach(l -> l.retrying(n, job.attempt(), delay));
                System.out.println("      retry " + n.id() + " in " + delay
                        + " ms, attempt " + job.attempt() + " failed: " + e.getMessage());
                retryClock.schedule(() -> queue.offer(new Job(n, job.attempt() + 1)),
                        delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void giveUp(Notification n, String reason, int attempts) {
        dlq.add(n, reason, attempts);
        listeners.forEach(l -> l.deadLettered(n, reason));
        inFlight.decrementAndGet();
    }

    public void drain(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (inFlight.get() > 0 && System.currentTimeMillis() < deadline) Thread.sleep(10);
    }

    public int rejected() { return rejected.get(); }
    public int duplicatesIgnored() { return duplicatesIgnored.get(); }

    @Override public void close() {
        running = false;
        retryClock.shutdownNow();
        workers.forEach(Thread::interrupt);
    }
}
