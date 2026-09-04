import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Ten thousand tasks, each waiting 100 ms for something slow. This is the shape
 * of a service that mostly calls other services.
 *
 * With a fixed pool the maths is simple and unforgiving: tasks divided by
 * threads, times how long each one waits. Virtual threads remove the divisor.
 *
 *   java BlockingThroughput.java
 */
public class BlockingThroughput {

    private static final int TASKS = 10_000;
    private static final int WAIT_MS = 100;

    public static void main(String[] args) throws Exception {
        System.out.printf("%,d tasks, each waiting %d ms for something slow%n%n", TASKS, WAIT_MS);
        System.out.printf("%-38s %12s   %s%n", "executor", "wall time", "in theory");
        System.out.println("-".repeat(76));

        run("fixed pool, 200 threads",
                Executors.newFixedThreadPool(200), TASKS / 200 * WAIT_MS);
        run("fixed pool, 1000 threads",
                Executors.newFixedThreadPool(1000), TASKS / 1000 * WAIT_MS);
        run("fixed pool, 2000 threads",
                Executors.newFixedThreadPool(2000), TASKS / 2000 * WAIT_MS);
        run("one virtual thread per task",
                Executors.newVirtualThreadPerTaskExecutor(), WAIT_MS);
    }

    private static void run(String label, ExecutorService pool, long theory) throws Exception {
        long t0 = System.currentTimeMillis();
        try (pool) {
            for (int i = 0; i < TASKS; i++) {
                pool.submit(() -> {
                    try {
                        Thread.sleep(WAIT_MS);      // stands in for a network call
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.MINUTES);
        }
        long ms = System.currentTimeMillis() - t0;
        System.out.printf("%-38s %9d ms   about %d ms%n", label, ms, theory);
    }
}
