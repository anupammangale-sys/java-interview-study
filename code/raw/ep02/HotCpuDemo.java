/**
 * One thread burning CPU in a tight loop. Three others doing nothing.
 *
 * This is the shape of a real "CPU is at 100 percent" incident: most threads
 * are fine, one is spinning, and you have to find which one.
 *
 *   java HotCpuDemo
 *   jstack <pid>     <- the busy thread is RUNNABLE, the idle ones are TIMED_WAITING
 */
public class HotCpuDemo {

    public static void main(String[] args) throws Exception {
        // the guilty one
        Thread hot = new Thread(HotCpuDemo::spin, "report-builder");
        hot.setDaemon(true);
        hot.start();

        // three innocent bystanders
        for (int i = 1; i <= 3; i++) {
            Thread idle = new Thread(HotCpuDemo::rest, "worker-" + i);
            idle.setDaemon(true);
            idle.start();
        }

        System.out.println("pid = " + ProcessHandle.current().pid());
        System.out.println("one thread is burning CPU. take a thread dump.");
        Thread.sleep(10 * 60 * 1000);
    }

    /** Looks harmless. Recomputes the same thing forever and never sleeps. */
    private static void spin() {
        long total = 0;
        while (true) {
            for (int i = 0; i < 1_000_000; i++) {
                total += i % 7;
            }
            if (total == Long.MIN_VALUE) {
                System.out.println("never happens, stops the compiler removing the loop");
            }
        }
    }

    private static void rest() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
