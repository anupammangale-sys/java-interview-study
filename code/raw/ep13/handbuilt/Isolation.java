import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The two patterns that are about resources rather than about failure:
 * a bulkhead, which stops one sick dependency eating the whole thread pool,
 * and a timeout, which stops a call holding a thread for ever.
 *
 *   java Isolation.java
 */
public class Isolation {

    static void head(String s) {
        System.out.println();
        System.out.println("=== " + s + " ===");
    }

    public static void main(String[] args) throws Exception {
        bulkhead();
        timeout();
    }

    // ------------------------------------------------------------------ 4

    /** A bulkhead is a permit count. That is genuinely all it is. */
    static class Bulkhead {
        private final Semaphore permits;
        private final AtomicInteger rejected = new AtomicInteger();
        Bulkhead(int n) { this.permits = new Semaphore(n); }

        <T> T call(Callable<T> work) throws Exception {
            if (!permits.tryAcquire()) {
                rejected.incrementAndGet();
                throw new RejectedExecutionException("bulkhead full");
            }
            try { return work.call(); } finally { permits.release(); }
        }
        int rejected() { return rejected.get(); }
    }

    private static void bulkhead() throws Exception {
        head("4. one sick dependency, one shared thread pool");
        System.out.println("  A pool of 10 threads serves two dependencies.");
        System.out.println("  The reports service has gone slow: 2 seconds a call.");
        System.out.println("  The prices service is fine: 10 ms a call.");
        System.out.println("  20 report requests arrive, then 20 price requests.");
        System.out.println();

        System.out.printf("  %-26s %14s %14s %14s%n",
                "", "report calls", "price calls", "slowest price");
        System.out.printf("  %-26s %14s %14s %14s%n",
                "", "refused", "that worked", "wait, submit");
        System.out.printf("  %-26s %14s %14s %14s%n",
                "", "", "", "to done");
        System.out.println("  " + "-".repeat(70));

        run("no bulkhead", null);
        run("bulkhead, 4 permits", new Bulkhead(4));

        System.out.println();
        System.out.println("  Without a bulkhead the slow dependency owns every thread, so");
        System.out.println("  calls to a perfectly healthy service queue behind it. The");
        System.out.println("  bulkhead refuses some report calls so the prices keep flowing.");
    }

    private static void run(String label, Bulkhead bulkhead) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        AtomicInteger priceOk = new AtomicInteger();
        AtomicInteger slowestPrice = new AtomicInteger();
        List<Future<?>> all = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            all.add(pool.submit(() -> {
                try {
                    Callable<String> work = () -> { FlakyService.sleep(2000); return "report"; };
                    if (bulkhead == null) work.call(); else bulkhead.call(work);
                } catch (RejectedExecutionException e) {
                    // the bulkhead refused it, which is the point
                } catch (Exception ignored) { }
            }));
        }

        FlakyService.sleep(50);          // let the reports get in first

        for (int i = 0; i < 20; i++) {
            // measured from SUBMISSION, not from when the task starts running.
            // The whole cost of a starved pool is the wait before it runs, and
            // timing from inside the task hides exactly that.
            final long submittedAt = System.nanoTime();
            all.add(pool.submit(() -> {
                FlakyService.sleep(10);
                priceOk.incrementAndGet();
                int ms = (int) ((System.nanoTime() - submittedAt) / 1_000_000);
                slowestPrice.accumulateAndGet(ms, Math::max);
            }));
        }

        for (Future<?> f : all) f.get();
        pool.shutdown();

        System.out.printf("  %-26s %14d %14d %11d ms%n",
                label, bulkhead == null ? 0 : bulkhead.rejected(), priceOk.get(),
                slowestPrice.get());
    }

    // ------------------------------------------------------------------ 5

    private static void timeout() throws Exception {
        head("5. a call that never comes back");

        ExecutorService pool = Executors.newFixedThreadPool(2);

        long start = System.nanoTime();
        Future<String> slow = pool.submit(() -> { FlakyService.sleep(3000); return "eventually"; });
        String answer = slow.get();
        long waited = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("  no timeout      waited %d ms, got \"%s\"%n", waited, answer);

        start = System.nanoTime();
        Future<String> slow2 = pool.submit(() -> { FlakyService.sleep(3000); return "eventually"; });
        String result;
        try {
            result = slow2.get(200, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            result = "gave up";
            slow2.cancel(true);
        }
        long waited2 = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("  200 ms timeout  waited %d ms, got \"%s\"%n", waited2, result);

        pool.shutdownNow();
        System.out.println();
        System.out.println("  The timeout freed the CALLER after " + waited2 + " ms.");
        System.out.println("  Whether the work itself stopped is a separate question: cancel");
        System.out.println("  only interrupts, and code that ignores interruption keeps going.");
        System.out.println("  A timeout with no way to stop the work still leaks threads.");
    }
}
