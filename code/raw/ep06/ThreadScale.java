import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * How many threads can you actually have, and what does starting them cost?
 *
 * Each thread does nothing but wait, so this measures the cost of existing
 * rather than the cost of working. Platform threads each need about a megabyte
 * of memory outside the heap. Virtual threads are objects on the heap.
 *
 * Run each kind in its OWN JVM. Tearing down fifty thousand platform threads
 * takes long enough to contaminate whatever is measured next, which is exactly
 * what happened the first time this was written:
 *
 *   java ThreadScale.java platform
 *   java ThreadScale.java virtual
 */
public class ThreadScale {

    public static void main(String[] args) {
        boolean virtual = args.length > 0 && args[0].equals("virtual");
        String kind = virtual ? "virtual" : "platform";

        System.out.printf("%s threads that only wait%n%n", kind);
        System.out.printf("%-12s %12s %14s   %s%n", "asked for", "started", "time to start", "note");
        System.out.println("-".repeat(62));

        int[] counts = virtual
                ? new int[]{1_000, 10_000, 100_000, 500_000, 1_000_000}
                : new int[]{1_000, 10_000, 20_000, 50_000};

        for (int n : counts) {
            if (!attempt(n, virtual)) break;   // stop once the machine says no
        }
    }

    /** @return false if we could not create everything asked for. */
    private static boolean attempt(int wanted, boolean virtual) {
        CountDownLatch release = new CountDownLatch(1);
        List<Thread> started = new ArrayList<>(wanted);
        String note = "";
        boolean complete = true;

        long t0 = System.nanoTime();
        try {
            for (int i = 0; i < wanted; i++) {
                Runnable body = () -> {
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                };
                Thread t = virtual
                        ? Thread.ofVirtual().unstarted(body)
                        : Thread.ofPlatform().daemon().unstarted(body);
                t.start();
                started.add(t);
            }
        } catch (OutOfMemoryError e) {
            note = "could not create any more";
            complete = false;
        } catch (Throwable e) {
            note = e.getClass().getSimpleName();
            complete = false;
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;

        System.out.printf("%,12d %,12d %11d ms   %s%n", wanted, started.size(), ms, note);

        // Release them and give the whole batch one shared deadline, rather than
        // waiting on each thread in turn.
        release.countDown();
        long deadline = System.currentTimeMillis() + 3_000;
        for (Thread t : started) {
            long left = deadline - System.currentTimeMillis();
            if (left <= 0) break;
            try {
                t.join(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return complete;
    }
}
