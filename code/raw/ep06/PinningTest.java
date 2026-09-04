import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Does blocking inside synchronized still trap the carrier thread?
 *
 * The classic advice is yes: a virtual thread that blocks inside a synchronized
 * block cannot unmount, so it holds its carrier hostage and you lose the whole
 * point of virtual threads. The usual fix given is to replace synchronized with
 * ReentrantLock.
 *
 * This measures it. Every virtual thread locks its OWN object, so there is no
 * contention at all: the only thing that could serialise them is pinning.
 *
 *   If pinning happens : time is about (threads / carriers) * sleep
 *   If it does not     : time is about sleep, whatever the thread count
 *
 *   java PinningTest.java
 */
public class PinningTest {

    private static final int THREADS = 2_000;
    private static final int SLEEP_MS = 200;

    public static void main(String[] args) throws Exception {
        int carriers = Runtime.getRuntime().availableProcessors();
        System.out.println("Java " + System.getProperty("java.version"));
        System.out.printf("%,d virtual threads, each holding its OWN lock and sleeping %d ms%n",
                THREADS, SLEEP_MS);
        System.out.printf("Carrier threads available: %d%n", carriers);
        System.out.println();
        System.out.printf("if pinning happens, expect roughly %,d ms%n",
                (long) THREADS / carriers * SLEEP_MS);
        System.out.printf("if it does not, expect roughly %,d ms%n", SLEEP_MS);
        System.out.println();
        System.out.printf("%-34s %12s%n", "blocking inside", "wall time");
        System.out.println("-".repeat(50));

        // warm up so class loading is not counted
        measure(true);
        measure(false);

        System.out.printf("%-34s %9d ms%n", "synchronized (own monitor)", measure(true));
        System.out.printf("%-34s %9d ms%n", "ReentrantLock (own lock)", measure(false));
    }

    private static long measure(boolean useSynchronized) throws Exception {
        CountDownLatch done = new CountDownLatch(THREADS);
        long t0 = System.currentTimeMillis();

        for (int i = 0; i < THREADS; i++) {
            final Object monitor = new Object();          // nobody else wants these
            final ReentrantLock lock = new ReentrantLock();
            Thread.ofVirtual().start(() -> {
                try {
                    if (useSynchronized) {
                        synchronized (monitor) {
                            Thread.sleep(SLEEP_MS);
                        }
                    } else {
                        lock.lock();
                        try {
                            Thread.sleep(SLEEP_MS);
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        done.await();
        return System.currentTimeMillis() - t0;
    }
}
