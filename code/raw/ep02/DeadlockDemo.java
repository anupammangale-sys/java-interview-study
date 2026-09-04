/**
 * Two threads, two locks, taken in opposite order.
 *
 * Thread A takes lock 1, then wants lock 2.
 * Thread B takes lock 2, then wants lock 1.
 * Neither will ever let go. Both wait forever.
 *
 *   java DeadlockDemo
 *   jstack <pid>        <- look for "Found one Java-level deadlock"
 */
public class DeadlockDemo {

    private static final Object ACCOUNT_A = new Object();
    private static final Object ACCOUNT_B = new Object();

    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(() -> {
            synchronized (ACCOUNT_A) {
                sleep(100);                 // give the other thread time to grab B
                synchronized (ACCOUNT_B) {  // never gets here
                    System.out.println("transfer A to B done");
                }
            }
        }, "transfer-A-to-B");

        Thread t2 = new Thread(() -> {
            synchronized (ACCOUNT_B) {
                sleep(100);
                synchronized (ACCOUNT_A) {  // never gets here
                    System.out.println("transfer B to A done");
                }
            }
        }, "transfer-B-to-A");

        t1.start();
        t2.start();

        System.out.println("pid = " + ProcessHandle.current().pid());
        System.out.println("both threads are now stuck. take a thread dump.");

        t1.join();
        t2.join();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
