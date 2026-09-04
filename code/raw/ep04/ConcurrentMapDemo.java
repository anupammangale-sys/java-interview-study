import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * What actually happens when several threads write to a plain HashMap.
 *
 * Every thread writes a distinct set of keys, so there are no overwrites. If
 * the map behaved correctly the final size would always be threads * perThread.
 * It is not.
 *
 *   java ConcurrentMapDemo.java
 */
public class ConcurrentMapDemo {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 20_000;
    private static final int EXPECTED = THREADS * PER_THREAD;
    private static final int ATTEMPTS = 5;
    private static final int TIMEOUT_SECONDS = 10;

    public static void main(String[] args) throws Exception {
        System.out.printf("%d threads, %d distinct keys each, so a correct map ends with %d entries%n%n",
                THREADS, PER_THREAD, EXPECTED);

        run("HashMap (no protection)", HashMap::new);
        run("Collections.synchronizedMap", () -> Collections.synchronizedMap(new HashMap<>()));
        run("ConcurrentHashMap", ConcurrentHashMap::new);
        System.exit(0);   // abandon any threads still spinning inside a broken HashMap
    }

    private static void run(String label, java.util.function.Supplier<Map<String, Integer>> factory)
            throws Exception {
        System.out.println(label);
        List<String> results = new ArrayList<>();
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            Map<String, Integer> map = factory.get();
            String outcome;
            long ms;
            try {
                ms = fill(map);
                int size = map.size();
                outcome = size == EXPECTED
                        ? String.format("%,d entries  correct     (%d ms)", size, ms)
                        : String.format("%,d entries  LOST %,d  (%d ms)", size, EXPECTED - size, ms);
            } catch (IllegalStateException e) {
                outcome = "DID NOT FINISH - threads still spinning after "
                        + TIMEOUT_SECONDS + "s (the map is corrupted)";
            } catch (Exception | Error e) {
                outcome = "threw " + e.getClass().getSimpleName();
            }
            results.add(outcome);
            System.out.printf("  attempt %d: %s%n", attempt, outcome);
        }
        System.out.println();
    }

    private static long fill(Map<String, Integer> map) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS, r -> {
            Thread th = new Thread(r);
            th.setDaemon(true);   // a spinning HashMap must not keep the JVM alive
            return th;
        });
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        for (int t = 0; t < THREADS; t++) {
            int id = t;
            pool.submit(() -> {
                try {
                    go.await();
                    for (int i = 0; i < PER_THREAD; i++) {
                        map.put("t" + id + "-k" + i, i);   // every key is unique
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        long start = System.currentTimeMillis();
        go.countDown();
        boolean finished = done.await(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        long ms = System.currentTimeMillis() - start;
        pool.shutdownNow();
        if (!finished) {
            throw new IllegalStateException("did not finish");
        }
        return ms;
    }
}
