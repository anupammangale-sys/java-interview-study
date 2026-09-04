import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Looks inside a real HashMap using reflection, so the internals are observed
 * rather than described.
 *
 * Needs the java.util package opened up, because modern Java hides internals
 * by default:
 *
 *   java --add-opens java.base/java.util=ALL-UNNAMED HashMapInternals.java
 */
public class HashMapInternals {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. capacity as entries are added ===");
        capacityGrowth();

        System.out.println();
        System.out.println("=== 2. where keys actually land ===");
        bucketSpread();

        System.out.println();
        System.out.println("=== 3. what happens when every key collides ===");
        collisions();
    }

    /** Capacity doubles once size passes capacity * 0.75. */
    private static void capacityGrowth() throws Exception {
        Map<Integer, String> map = new HashMap<>();
        int lastCapacity = -1;
        for (int i = 1; i <= 100; i++) {
            map.put(i, "v");
            int cap = capacityOf(map);
            if (cap != lastCapacity) {
                System.out.printf("size %3d -> capacity became %3d  (resizes when size passes %d)%n",
                        i, cap, (int) (cap * 0.75));
                lastCapacity = cap;
            }
        }
    }

    /** How evenly do normal keys spread across the buckets? */
    private static void bucketSpread() throws Exception {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            map.put("user-" + i, "v");
        }
        Object[] table = tableOf(map);
        int empty = 0, used = 0, longest = 0;
        for (Object bucket : table) {
            if (bucket == null) {
                empty++;
                continue;
            }
            used++;
            longest = Math.max(longest, chainLength(bucket));
        }
        System.out.printf("1000 keys in a table of %d buckets%n", table.length);
        System.out.printf("  buckets used   : %d%n", used);
        System.out.printf("  buckets empty  : %d%n", empty);
        System.out.printf("  longest chain  : %d entries%n", longest);
    }

    /** Every key hashes to the same bucket, so the chain grows and then changes shape. */
    private static void collisions() throws Exception {
        Map<Collider, String> map = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            map.put(new Collider(i), "v");
            Object[] table = tableOf(map);
            Object bucket = firstNonNull(table);
            System.out.printf("after %2d puts: table=%3d  chain=%2d  bucket type = %s%n",
                    i, table.length, chainLength(bucket), typeName(bucket));
        }
    }

    /** All instances land in the same bucket, on purpose. */
    record Collider(int id) {
        @Override
        public int hashCode() {
            return 42;
        }
    }

    // ---- reflection helpers ----

    private static Object[] tableOf(Map<?, ?> map) throws Exception {
        Field f = HashMap.class.getDeclaredField("table");
        f.setAccessible(true);
        return (Object[]) f.get(map);
    }

    private static int capacityOf(Map<?, ?> map) throws Exception {
        Object[] table = tableOf(map);
        return table == null ? 0 : table.length;
    }

    private static Object firstNonNull(Object[] table) {
        for (Object o : table) {
            if (o != null) return o;
        }
        return null;
    }

    private static String typeName(Object bucket) {
        if (bucket == null) return "empty";
        String n = bucket.getClass().getSimpleName();
        return n.contains("TreeNode") ? n + "   <-- became a tree" : n;
    }

    private static int chainLength(Object node) {
        int n = 0;
        try {
            while (node != null) {
                n++;
                Field next = node.getClass().getDeclaredField("next");
                next.setAccessible(true);
                node = next.get(node);
            }
        } catch (Exception e) {
            return n;   // tree nodes are linked differently; count what we can
        }
        return n;
    }
}
