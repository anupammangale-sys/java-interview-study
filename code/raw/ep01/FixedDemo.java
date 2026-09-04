import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The fix, without pulling in Caffeine - a bounded LRU in 4 lines.
 * In a real service you would use Caffeine with maximumSize + expireAfterWrite.
 *
 * Point to make on camera: the leak was never "too little memory".
 * It was an unbounded lifetime. Bound the lifetime, and 128 MB is plenty.
 */
public class FixedDemo {

    private static final int MAX_ENTRIES = 1000;

    private static final Map<String, byte[]> CACHE =
        new LinkedHashMap<String, byte[]>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                return size() > MAX_ENTRIES;
            }
        };

    public static void main(String[] args) throws Exception {
        int i = 0;
        while (true) {
            CACHE.put("order-" + i, new byte[64 * 1024]);
            i++;

            Thread.sleep(15); // video pacing

            if (i % 50 == 0) {
                Runtime rt = Runtime.getRuntime();
                long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                System.out.println("cached=" + i + "   entries held=" + CACHE.size()
                    + "   heap used=" + usedMb + " MB");
            }
        }
    }
}
