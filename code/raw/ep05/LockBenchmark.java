import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * The same shared state, guarded four different ways, under a read heavy load:
 * seven threads reading, one writing. This is the shape of most real caches
 * and configuration holders.
 *
 * Rough measurement, not a rigorous benchmark: warmup rounds then best of
 * several. Good enough for differences of two times or more.
 *
 *   java LockBenchmark.java
 */
public class LockBenchmark {

    private static final int READERS = 7;
    private static final int WRITERS = 1;
    private static final int MILLIS = 700;
    private static final int WARMUP = 2;
    private static final int ROUNDS = 4;

    // the shared state every variant guards
    private static long a, b;

    /** How much work happens inside the read lock. 0 = just read two fields. */
    private static int readWork = 0;

    /** Stands in for real work done while holding the read lock. */
    private static long payload() {
        long v = a + b;
        for (int i = 0; i < readWork; i++) v += (v ^ i) % 7;
        return v;
    }

    public static void main(String[] args) throws Exception {
        System.out.printf("%d readers, %d writer, %d ms per round, best of %d%n",
                READERS, WRITERS, MILLIS, ROUNDS);

        for (int work : new int[]{0, 200}) {
            readWork = work;
            System.out.println();
            System.out.println(work == 0
                    ? "A. Tiny critical section: the read is just two field accesses"
                    : "B. Longer critical section: the read does some real work while holding the lock");
            System.out.printf("%-26s %16s %16s%n", "guarded by", "reads/sec", "writes/sec");
            System.out.println("-".repeat(62));
            synchronizedRun();
            reentrantRun();
            readWriteRun();
            stampedRun();
        }
    }

    private static void report(String label, long reads, long writes) {
        System.out.printf("%-26s %,16d %,16d%n", label, reads * 1000 / MILLIS, writes * 1000 / MILLIS);
    }

    // ---- 1. synchronized: readers block each other ----
    private static void synchronizedRun() throws Exception {
        Object lock = new Object();
        best("synchronized",
                () -> { synchronized (lock) { return payload(); } },
                () -> { synchronized (lock) { a++; b++; } });
    }

    // ---- 2. ReentrantLock: same, readers still block each other ----
    private static void reentrantRun() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        best("ReentrantLock",
                () -> { lock.lock(); try { return payload(); } finally { lock.unlock(); } },
                () -> { lock.lock(); try { a++; b++; } finally { lock.unlock(); } });
    }

    // ---- 3. ReadWriteLock: readers share, writers exclude ----
    private static void readWriteRun() throws Exception {
        ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
        best("ReentrantReadWriteLock",
                () -> { rw.readLock().lock(); try { return payload(); } finally { rw.readLock().unlock(); } },
                () -> { rw.writeLock().lock(); try { a++; b++; } finally { rw.writeLock().unlock(); } });
    }

    // ---- 4. StampedLock: optimistic reads take no lock at all ----
    private static void stampedRun() throws Exception {
        StampedLock sl = new StampedLock();
        best("StampedLock (optimistic)",
                () -> {
                    long stamp = sl.tryOptimisticRead();
                    long v = payload();
                    if (!sl.validate(stamp)) {          // a writer got in, redo it properly
                        stamp = sl.readLock();
                        try { v = payload(); } finally { sl.unlockRead(stamp); }
                    }
                    return v;
                },
                () -> { long stamp = sl.writeLock(); try { a++; b++; } finally { sl.unlockWrite(stamp); } });
    }

    interface Read { long get(); }

    private static void best(String label, Read read, Runnable write) throws Exception {
        long bestReads = 0, bestWrites = 0;
        for (int r = 0; r < WARMUP + ROUNDS; r++) {
            long[] counts = round(read, write);
            if (r >= WARMUP && counts[0] > bestReads) {
                bestReads = counts[0];
                bestWrites = counts[1];
            }
        }
        report(label, bestReads, bestWrites);
    }

    private static long[] round(Read read, Runnable write) throws Exception {
        a = 0; b = 0;
        AtomicLong reads = new AtomicLong(), writes = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(READERS + WRITERS);
        CountDownLatch go = new CountDownLatch(1);
        volatileStop = false;

        for (int i = 0; i < READERS; i++) {
            pool.submit(() -> {
                try { go.await(); } catch (InterruptedException e) { return; }
                long n = 0, sink = 0;
                while (!volatileStop) { sink += read.get(); n++; }
                if (sink == Long.MIN_VALUE) System.out.print("");
                reads.addAndGet(n);
            });
        }
        for (int i = 0; i < WRITERS; i++) {
            pool.submit(() -> {
                try { go.await(); } catch (InterruptedException e) { return; }
                long n = 0;
                while (!volatileStop) { write.run(); n++; }
                writes.addAndGet(n);
            });
        }

        go.countDown();
        Thread.sleep(MILLIS);
        volatileStop = true;
        pool.shutdown();
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        return new long[]{reads.get(), writes.get()};
    }

    private static volatile boolean volatileStop;
}
