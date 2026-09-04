import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * What a thread pool actually does when more work arrives than it can take.
 *
 * 2 threads, a queue that holds 4, and 20 tasks submitted as fast as possible.
 * So at most 6 tasks can be accepted at any moment and the other 14 have to go
 * somewhere. Where they go is the rejection policy, and the four built in ones
 * behave very differently.
 *
 *   java ExecutorBehaviour.java
 */
public class ExecutorBehaviour {

    private static final int THREADS = 2;
    private static final int QUEUE = 4;
    private static final int TASKS = 20;

    public static void main(String[] args) throws Exception {
        System.out.printf("%d threads, queue holds %d, %d tasks submitted at once%n",
                THREADS, QUEUE, TASKS);
        System.out.println("So only " + (THREADS + QUEUE) + " can be accepted at a time. "
                + "The rest depend on the policy.%n".replace("%n", ""));
        System.out.println();
        System.out.printf("%-34s %8s %10s %14s   %s%n",
                "rejection policy", "ran", "rejected", "ran on caller", "effect");
        System.out.println("-".repeat(92));

        run("AbortPolicy (the default)", new ThreadPoolExecutor.AbortPolicy(),
                "throws at the caller, work is lost unless you catch it");
        run("CallerRunsPolicy", new ThreadPoolExecutor.CallerRunsPolicy(),
                "caller does the work itself, which slows the producer down");
        run("DiscardPolicy", new ThreadPoolExecutor.DiscardPolicy(),
                "silently dropped, no error anywhere");
        run("DiscardOldestPolicy", new ThreadPoolExecutor.DiscardOldestPolicy(),
                "drops the longest waiting task to make room");
    }

    private static void run(String label, RejectedExecutionHandler policy, String effect)
            throws Exception {
        AtomicInteger ran = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger onCaller = new AtomicInteger();
        final Thread main = Thread.currentThread();

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                THREADS, THREADS, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE), policy);

        for (int i = 0; i < TASKS; i++) {
            try {
                pool.execute(() -> {
                    if (Thread.currentThread() == main) onCaller.incrementAndGet();
                    ran.incrementAndGet();
                    try { Thread.sleep(20); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (RejectedExecutionException e) {
                rejected.incrementAndGet();
            }
        }

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        System.out.printf("%-34s %8d %10d %14d   %s%n",
                label, ran.get(), rejected.get(), onCaller.get(), effect);
    }
}
