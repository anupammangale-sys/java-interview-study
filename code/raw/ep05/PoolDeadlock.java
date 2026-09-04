import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A task that waits for another task on the SAME pool.
 *
 * Nothing here is obviously wrong. It is async code, using CompletableFuture,
 * on a normal fixed pool. It still stops forever, because the outer tasks
 * occupy every thread while waiting for inner tasks that need a thread to run.
 *
 *   java PoolDeadlock.java
 */
public class PoolDeadlock {

    private static final int TIMEOUT_SECONDS = 6;

    public static void main(String[] args) throws Exception {
        System.out.println("Each outer task waits on an inner task. Watch what actually fixes it.");
        System.out.println();

        attempt("2 threads, 2 tasks, same pool", 2, 2, true);
        attempt("4 threads, 2 tasks, same pool", 4, 2, true);
        attempt("4 threads, 4 tasks, same pool", 4, 4, true);
        attempt("8 threads, 8 tasks, same pool", 8, 8, true);
        attempt("2 threads, 8 tasks, separate inner pool", 2, 8, false);

        System.exit(0);   // abandon anything still stuck
    }

    private static void attempt(String label, int poolSize, int taskCount, boolean sharePool) {
        ExecutorService outer = Executors.newFixedThreadPool(poolSize, daemon());
        ExecutorService inner = sharePool ? outer : Executors.newFixedThreadPool(2, daemon());
        try {
            // Every outer task must be running before any of them asks for an
            // inner thread. Without this the result is a race: the pool creates
            // threads lazily, so an inner task can grab a thread that no outer
            // task has claimed yet, and the deadlock appears only sometimes.
            // Wait until the pool is saturated, not until every task has started:
            // with fewer threads than tasks, the latter can never happen.
            java.util.concurrent.CountDownLatch allStarted =
                    new java.util.concurrent.CountDownLatch(Math.min(taskCount, poolSize));

            CompletableFuture<?>[] all = new CompletableFuture<?>[taskCount];
            for (int i = 0; i < taskCount; i++) {
                all[i] = CompletableFuture.runAsync(() -> {
                    allStarted.countDown();
                    try {
                        allStarted.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    // the inner piece of work needs a thread from `inner`
                    CompletableFuture<String> child =
                            CompletableFuture.supplyAsync(() -> "done", inner);
                    child.join();          // blocks this thread until the child runs
                }, outer);
            }
            CompletableFuture.allOf(all).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            System.out.printf("  %-42s finished%n", label);
        } catch (Exception e) {
            System.out.printf("  %-42s STUCK, gave up after %ds (%s)%n",
                    label, TIMEOUT_SECONDS, e.getClass().getSimpleName());
        } finally {
            outer.shutdownNow();
            if (!sharePool) inner.shutdownNow();
        }
    }

    private static java.util.concurrent.ThreadFactory daemon() {
        return r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);   // a stuck pool must not keep the JVM alive
            return t;
        };
    }
}
