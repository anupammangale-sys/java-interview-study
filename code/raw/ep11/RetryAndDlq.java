import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/**
 * Two things every low level design round asks about once you mention retries.
 *
 * 1. What happens when everyone retries at the same moment.
 * 2. What it costs to retry something that can never succeed.
 *
 * The clock is simulated, so the numbers are about the algorithm and not
 * about how this machine felt on the day.
 *
 *   java RetryAndDlq.java
 */
public class RetryAndDlq {

    static final int CLIENTS = 500;
    static final long BASE_MS = 100;
    static final int MAX_ATTEMPTS = 4;

    /** Every client fails at t=0 and retries on the same schedule. */
    static List<Long> retryTimes(boolean jitter, long seed) {
        Random rnd = new Random(seed);
        List<Long> arrivals = new ArrayList<>();
        for (int c = 0; c < CLIENTS; c++) {
            long t = 0;
            for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
                long backoff = BASE_MS * (1L << (attempt - 1));   // 100, 200, 400
                long wait = jitter ? (long) (rnd.nextDouble() * backoff) : backoff;
                t += wait;
                arrivals.add(t);
            }
        }
        return arrivals;
    }

    /** The number that matters: the most retries landing in any 10 ms slice. */
    static int peakPer10ms(List<Long> arrivals) {
        TreeMap<Long, Integer> buckets = new TreeMap<>();
        for (long t : arrivals) buckets.merge(t / 10, 1, Integer::sum);
        return buckets.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    static int distinctSlices(List<Long> arrivals) {
        return (int) arrivals.stream().map(t -> t / 10).distinct().count();
    }

    static void storm() {
        System.out.println("=== 1. everyone fails at the same moment ===");
        System.out.println("  " + CLIENTS + " clients, up to " + (MAX_ATTEMPTS - 1)
                + " retries each, backoff " + BASE_MS + " ms doubling each time.");
        System.out.println();
        System.out.printf("  %-28s %10s %14s %16s%n",
                "", "retries", "10 ms slices", "worst slice");
        System.out.println("  " + "-".repeat(72));

        for (boolean jitter : new boolean[]{false, true}) {
            List<Long> a = retryTimes(jitter, 42);
            System.out.printf("  %-28s %10d %14d %16d%n",
                    jitter ? "with jitter" : "no jitter, plain backoff",
                    a.size(), distinctSlices(a), peakPer10ms(a));
        }
        System.out.println();
        System.out.println("  Plain backoff does not spread anything out. It moves the whole");
        System.out.println("  crowd together, so the server that just failed gets hit by all");
        System.out.println("  " + CLIENTS + " of them at once, three times over.");
        System.out.println();
    }

    /** A notification that either can be delivered later, or never can. */
    record Item(String id, boolean permanentlyBroken) {}

    static void classification() {
        System.out.println("=== 2. what it costs to retry something that cannot work ===");

        int total = 200, broken = 60;
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < total; i++) items.add(new Item("N-" + i, i < broken));

        for (boolean classify : new boolean[]{false, true}) {
            int attempts = 0, delivered = 0, deadLettered = 0;
            long clockMs = 0;

            for (Item item : items) {
                if (!item.permanentlyBroken()) {
                    attempts += 2;                    // busy once, then delivered
                    clockMs += BASE_MS;               // one backoff wait
                    delivered++;
                    continue;
                }
                if (classify) {
                    attempts += 1;                    // one look is enough
                    deadLettered++;
                } else {
                    attempts += MAX_ATTEMPTS;         // try, wait, try, wait, try, wait, give up
                    for (int a = 1; a < MAX_ATTEMPTS; a++) clockMs += BASE_MS * (1L << (a - 1));
                    deadLettered++;
                }
            }
            System.out.println();
            System.out.println("  " + (classify
                    ? "asking the exception whether a retry could ever work:"
                    : "retrying everything the same way:"));
            System.out.println("    attempts used        " + attempts);
            System.out.println("    delivered            " + delivered);
            System.out.println("    dead lettered        " + deadLettered);
            System.out.println("    time spent waiting   " + clockMs + " ms");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        storm();
        classification();
    }
}
