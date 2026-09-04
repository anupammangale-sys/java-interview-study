import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Eight threads each add 1 to a shared counter, half a million times.
 * A correct counter always ends at 4,000,000.
 *
 * Five ways of holding that counter, measured for correctness AND speed.
 * The volatile row is the one worth staring at.
 *
 *   java CounterRace.java
 */
public class CounterRace {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 500_000;
    private static final long EXPECTED = (long) THREADS * PER_THREAD;
    private static final int ROUNDS = 5;

    // the five counters
    private static long plain;
    private static volatile long vol;
    private static long guarded;
    private static final Object LOCK = new Object();
    private static final ReentrantLock RLOCK = new ReentrantLock();
    private static long locked;
    private static AtomicLong atomic = new AtomicLong();
    private static LongAdder adder = new LongAdder();

    public static void main(String[] args) throws Exception {
        System.out.printf("%d threads, %,d increments each, so a correct counter ends at %,d%n%n",
                THREADS, PER_THREAD, EXPECTED);
        System.out.printf("%-22s %14s %14s %10s   %s%n",
                "how the counter is held", "best result", "worst result", "best ms", "correct?");
        System.out.println("-".repeat(84));

        run("plain long",        () -> plain,        () -> plain = 0,        () -> plain++);
        run("volatile long",     () -> vol,          () -> vol = 0,          () -> vol++);
        run("synchronized",      () -> guarded,      () -> guarded = 0,
                () -> { synchronized (LOCK) { guarded++; } });
        run("ReentrantLock",     () -> locked,       () -> locked = 0,
                () -> { RLOCK.lock(); try { locked++; } finally { RLOCK.unlock(); } });
        run("AtomicLong",        () -> atomic.get(), () -> atomic = new AtomicLong(),
                () -> atomic.incrementAndGet());
        run("LongAdder",         () -> adder.sum(),  () -> adder = new LongAdder(),
                () -> adder.increment());
    }

    private static void run(String label, Supplier read, Runnable reset, Runnable increment)
            throws Exception {
        long best = Long.MAX_VALUE, worst = Long.MIN_VALUE, bestMs = Long.MAX_VALUE;
        boolean allCorrect = true;
        for (int r = 0; r < ROUNDS; r++) {
            reset.run();
            long ms = hammer(increment);
            long got = read.get();
            best = Math.max(best == Long.MAX_VALUE ? got : Math.max(best, got), got);
            worst = (worst == Long.MIN_VALUE) ? got : Math.min(worst, got);
            bestMs = Math.min(bestMs, ms);
            if (got != EXPECTED) allCorrect = false;
        }
        System.out.printf("%-22s %,14d %,14d %10d   %s%n",
                label, best, worst, bestMs,
                allCorrect ? "yes" : "NO, lost updates");
    }

    private static long hammer(Runnable increment) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        for (int t = 0; t < THREADS; t++) {
            pool.submit(() -> {
                try {
                    go.await();
                    for (int i = 0; i < PER_THREAD; i++) increment.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        long start = System.currentTimeMillis();
        go.countDown();
        done.await();
        long ms = System.currentTimeMillis() - start;
        pool.shutdownNow();
        return ms;
    }

    interface Supplier { long get(); }
}
