import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A pool with 3 threads. Ten jobs arrive. Every job calls something slow.
 *
 * All 3 threads end up stuck in the same place, and the other 7 jobs sit in
 * the queue waiting for a thread that is never coming back. The service looks
 * frozen, but memory is completely fine.
 *
 * In the thread dump this is unmistakable: several threads with the SAME name
 * pattern and the SAME stack trace, all in the same state.
 *
 *   java PoolStarvationDemo
 *   jstack <pid>
 */
public class PoolStarvationDemo {

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);

        for (int i = 1; i <= 10; i++) {
            int job = i;
            pool.submit(() -> {
                System.out.println("job " + job + " started on " + Thread.currentThread().getName());
                callSlowService();          // never returns
            });
        }

        Thread.sleep(2000);
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) pool;

        System.out.println();
        System.out.println("pid            = " + ProcessHandle.current().pid());
        System.out.println("pool threads   = " + tpe.getPoolSize());
        System.out.println("jobs running   = " + tpe.getActiveCount());
        System.out.println("jobs waiting   = " + tpe.getQueue().size());
        System.out.println();
        System.out.println("3 threads stuck, 7 jobs queued. take a thread dump.");

        Thread.sleep(10 * 60 * 1000);
    }

    /** Stands in for a payment gateway or a database with no timeout set. */
    private static void callSlowService() {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
