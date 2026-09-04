import java.util.ArrayList;
import java.util.List;

/**
 * A workload shaped like a real service: a lot of short-lived garbage, plus a
 * smaller set of objects that stick around for a while.
 *
 * The short-lived objects are what the young collection is designed for. The
 * surviving ones are what gets promoted to the old generation, which is where
 * the expensive collections come from.
 *
 * Run it under different collectors and compare:
 *   java -Xmx512m -XX:+UseSerialGC   -Xlog:gc GcDemo.java
 *   java -Xmx512m -XX:+UseParallelGC -Xlog:gc GcDemo.java
 *   java -Xmx512m -XX:+UseG1GC       -Xlog:gc GcDemo.java
 */
public class GcDemo {

    /** Objects that survive long enough to be promoted out of the young area. */
    private static final List<byte[]> liveSet = new ArrayList<>();

    private static final int LIVE_SET_MAX = 4200;   // roughly 135 MB held
    private static final int ITERATIONS = 1_500_000;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            // Dies immediately. This is what a young collection cleans up cheaply.
            byte[] garbage = new byte[16 * 1024];
            garbage[0] = (byte) i;

            // One in sixty survives for a while, so the live set keeps turning over.
            if (i % 60 == 0) {
                liveSet.add(new byte[32 * 1024]);
                if (liveSet.size() > LIVE_SET_MAX) {
                    liveSet.remove(0);
                }
            }
        }

        long ms = System.currentTimeMillis() - start;
        System.out.println("finished in " + ms + " ms, still holding " + liveSet.size() + " objects");
    }
}
