import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Archetype #3: ThreadLocal not removed on a pooled thread.
 *
 * 200 threads, like a Tomcat pool. Each task puts 1 MB into a ThreadLocal.
 * The tasks finish. The threads do NOT die - they go back to the pool.
 * Every live thread keeps its ThreadLocal map, so every 1 MB payload stays alive.
 *
 *   java -Xmx512m ThreadLocalLeakDemo.java          -> leaks
 *   java -Xmx512m ThreadLocalLeakDemo.java fixed    -> remove() in finally
 */
public class ThreadLocalLeakDemo {

    private static final ThreadLocal<byte[]> CONTEXT = new ThreadLocal<>();

    private static final int THREADS = 200;
    private static final int PAYLOAD = 1024 * 1024; // 1 MB per request

    public static void main(String[] args) throws Exception {
        boolean cleanUp = args.length > 0 && args[0].equals("fixed");

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch allStarted = new CountDownLatch(THREADS);
        CountDownLatch release = new CountDownLatch(1);

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    CONTEXT.set(new byte[PAYLOAD]); // "request context"
                    allStarted.countDown();
                    release.await();                // hold, so all 200 threads exist
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (cleanUp) {
                        CONTEXT.remove();           // <-- the entire fix
                    }
                }
            });
        }

        allStarted.await();
        release.countDown();
        Thread.sleep(1000); // let every task actually finish

        System.gc();
        Thread.sleep(500);

        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        System.out.println();
        System.out.println("  mode                     : " + (cleanUp ? "WITH remove()" : "WITHOUT remove()"));
        System.out.println("  tasks finished           : " + THREADS);
        System.out.println("  threads still alive      : " + THREADS + " (back in the pool)");
        System.out.println("  heap still held after GC : " + usedMb + " MB");
        System.out.println();

        pool.shutdownNow();
    }
}
