import java.util.HashMap;
import java.util.Map;

/**
 * Archetype #1: static collection used as a cache, with no eviction.
 *
 * CACHE is a static field, so it is a GC root. Everything it points to is
 * reachable forever. The garbage collector is not "failing" here - it is
 * correctly refusing to free objects that are still referenced.
 */
public class LeakDemo {

    private static final Map<String, byte[]> CACHE = new HashMap<>();

    public static void main(String[] args) throws Exception {
        int i = 0;
        while (true) {
            CACHE.put("order-" + i, new byte[64 * 1024]); // 64 KB per "order"
            i++;

            Thread.sleep(15); // video pacing - remove for a real repro

            if (i % 50 == 0) {
                Runtime rt = Runtime.getRuntime();
                long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                System.out.println("cached=" + i + "   heap used=" + usedMb + " MB");
            }
        }
    }
}
