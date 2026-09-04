import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Four ways to create a singleton lazily, hit by many threads at once.
 * A correct one gives exactly ONE instance no matter how many threads ask.
 *
 * Honest note about what this does and does not show. It demonstrates the
 * race everyone can reproduce: several threads passing the null check together
 * and each building their own object. It does NOT demonstrate the subtler
 * problem that `volatile` exists to fix, where one thread sees a reference to
 * an object whose fields are not written yet. That needs a processor with
 * weaker memory ordering than the usual desktop one, so claiming to have shown
 * it here would be dishonest.
 *
 *   java LazyInitRace.java
 */
public class LazyInitRace {

    private static final int THREADS = 32;
    private static final int ATTEMPTS = 5;

    static class Heavy {
        Heavy() {
            // enough work that two threads can overlap inside the constructor
            try { Thread.sleep(2); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // 1. no protection at all
    static Heavy unsafe;
    static Heavy getUnsafe() {
        if (unsafe == null) unsafe = new Heavy();
        return unsafe;
    }

    // 2. synchronize the whole method
    static Heavy syncd;
    static synchronized Heavy getSynchronized() {
        if (syncd == null) syncd = new Heavy();
        return syncd;
    }

    // 3. double checked locking, with volatile
    static volatile Heavy dcl;
    static Heavy getDoubleChecked() {
        Heavy local = dcl;
        if (local == null) {
            synchronized (LazyInitRace.class) {
                local = dcl;
                if (local == null) {
                    dcl = local = new Heavy();
                }
            }
        }
        return local;
    }

    // 4. the holder idiom: the class loader does the work
    static class Holder {
        static final Heavy INSTANCE = new Heavy();
    }
    static Heavy getHolder() {
        return Holder.INSTANCE;
    }

    interface Getter { Heavy get(); }

    public static void main(String[] args) throws Exception {
        System.out.printf("%d threads all calling getInstance at the same moment, %d attempts%n%n",
                THREADS, ATTEMPTS);
        System.out.printf("%-34s %22s   %s%n", "how it is written", "distinct instances", "correct?");
        System.out.println("-".repeat(78));

        run("no protection", () -> { unsafe = null; }, LazyInitRace::getUnsafe);
        run("synchronized method", () -> { syncd = null; }, LazyInitRace::getSynchronized);
        run("double checked + volatile", () -> { dcl = null; }, LazyInitRace::getDoubleChecked);
        run("holder idiom", () -> {}, LazyInitRace::getHolder);
    }

    private static void run(String label, Runnable reset, Getter getter) throws Exception {
        StringBuilder counts = new StringBuilder();
        boolean allCorrect = true;

        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            reset.run();
            Set<Heavy> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            CountDownLatch go = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(THREADS);

            for (int i = 0; i < THREADS; i++) {
                pool.submit(() -> {
                    try {
                        go.await();
                        Heavy h = getter.get();
                        synchronized (distinct) { distinct.add(h); }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            go.countDown();
            done.await();
            pool.shutdownNow();

            int n = distinct.size();
            if (n != 1) allCorrect = false;
            counts.append(n).append(attempt < ATTEMPTS - 1 ? ", " : "");
        }

        System.out.printf("%-34s %22s   %s%n", label, counts,
                allCorrect ? "yes" : "NO, several objects created");
    }
}
