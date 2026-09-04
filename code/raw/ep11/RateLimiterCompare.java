import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Three rate limiters, given exactly the same requests.
 *
 * The clock is a plain long that the test moves forward, not the real one.
 * That is deliberate: a test that sleeps is slow and flaky, and the point
 * here is what the algorithm decides, not how the machine schedules threads.
 *
 *   java RateLimiterCompare.java
 */
public class RateLimiterCompare {

    static final int LIMIT = 10;          // allowed per second
    static final long WINDOW = 1000;      // one second

    interface Limiter {
        String name();
        boolean allow(String client, long nowMs);
    }

    /** Counts per fixed block of time. The cheapest, and the one with a hole in it. */
    static class FixedWindow implements Limiter {
        private final Map<String, long[]> state = new HashMap<>();   // [windowId, count]
        public String name() { return "fixed window"; }
        public boolean allow(String client, long now) {
            long window = now / WINDOW;
            long[] s = state.computeIfAbsent(client, k -> new long[]{window, 0});
            if (s[0] != window) { s[0] = window; s[1] = 0; }
            if (s[1] < LIMIT) { s[1]++; return true; }
            return false;
        }
    }

    /** Keeps every timestamp in the last second. Exact, and the most expensive. */
    static class SlidingWindowLog implements Limiter {
        private final Map<String, Deque<Long>> state = new HashMap<>();
        public String name() { return "sliding window log"; }
        public boolean allow(String client, long now) {
            Deque<Long> times = state.computeIfAbsent(client, k -> new ArrayDeque<>());
            while (!times.isEmpty() && times.peekFirst() <= now - WINDOW) times.pollFirst();
            if (times.size() < LIMIT) { times.addLast(now); return true; }
            return false;
        }
    }

    /**
     * Refills steadily and lets a saved up burst through on purpose.
     *
     * Capacity and refill rate are two separate dials, and that is the whole
     * point of this algorithm. Capacity says how big a burst you will forgive
     * after a quiet spell. Rate says what you will sustain. Setting capacity
     * equal to the rate throws the feature away.
     */
    static class TokenBucket implements Limiter {
        private final Map<String, double[]> state = new HashMap<>();  // [tokens, lastRefillMs]
        private final int capacity;
        private final double perMs;
        TokenBucket(int capacity, int ratePerSecond) {
            this.capacity = capacity;
            this.perMs = (double) ratePerSecond / WINDOW;
        }
        public String name() { return "token bucket " + capacity + "/" + LIMIT; }
        public boolean allow(String client, long now) {
            double[] s = state.computeIfAbsent(client, k -> new double[]{capacity, now});
            s[0] = Math.min(capacity, s[0] + (now - s[1]) * perMs);
            s[1] = now;
            if (s[0] >= 1) { s[0] -= 1; return true; }
            return false;
        }
    }

    /** The measurement that matters: the most it ever let through in any one second. */
    static int worstSecond(List<Long> allowed) {
        int worst = 0;
        for (int i = 0; i < allowed.size(); i++) {
            int n = 0;
            for (int j = i; j < allowed.size(); j++) {
                if (allowed.get(j) - allowed.get(i) < WINDOW) n++; else break;
            }
            worst = Math.max(worst, n);
        }
        return worst;
    }

    static void run(String title, List<Long> requests, String explain) {
        System.out.println("=== " + title + " ===");
        System.out.println("  " + requests.size() + " requests. The limit is "
                + LIMIT + " per second.");
        System.out.println("  " + explain);
        System.out.println();
        System.out.printf("  %-20s %10s %10s   %s%n",
                "limiter", "allowed", "worst 1s", "verdict");
        System.out.println("  " + "-".repeat(62));

        for (Limiter lim : List.of(new FixedWindow(), new SlidingWindowLog(),
                                   new TokenBucket(20, LIMIT))) {
            List<Long> allowed = new ArrayList<>();
            for (long t : requests) if (lim.allow("client-a", t)) allowed.add(t);
            int worst = worstSecond(allowed);
            System.out.printf("  %-20s %10d %10d   %s%n",
                    lim.name(), allowed.size(), worst,
                    worst > LIMIT ? "over " + LIMIT + " by " + (worst - LIMIT) : "never over " + LIMIT);
        }
        System.out.println();
    }

    public static void main(String[] args) {
        // 1. steady traffic, well inside the limit
        List<Long> steady = new ArrayList<>();
        for (long t = 0; t < 3000; t += 200) steady.add(t);
        run("steady traffic, 5 per second", steady,
            "One request every 200 ms for three seconds. Nothing should be blocked.");

        // 2. the boundary. 10 at the end of one window, 10 at the start of the next.
        List<Long> boundary = new ArrayList<>();
        for (int i = 0; i < 10; i++) boundary.add(950L + i);     // 950 to 959
        for (int i = 0; i < 10; i++) boundary.add(1000L + i);    // 1000 to 1009
        run("a burst straddling a window boundary", boundary,
            "10 requests just before the 1000 ms mark and 10 just after, so 20 inside 60 ms.");

        // 4. the same burst, slid across the window, to see how much the answer
        //    depends on where the traffic happens to land
        System.out.println("=== does the answer depend on luck? ===");
        System.out.println("  The same burst of 20 requests, slid one millisecond at a");
        System.out.println("  time across a whole window. If a limiter is predictable, the");
        System.out.println("  worst second is the same wherever the burst lands.");
        System.out.println();
        System.out.printf("  %-20s %14s %14s%n", "limiter", "best case", "worst case");
        System.out.println("  " + "-".repeat(52));
        for (int which = 0; which < 3; which++) {
            int lo = 999, hi = 0;
            for (long offset = 0; offset < 1000; offset += 1) {
                Limiter lim = which == 0 ? new FixedWindow()
                            : which == 1 ? new SlidingWindowLog()
                            : new TokenBucket(20, LIMIT);
                List<Long> reqs = new ArrayList<>();
                for (int i = 0; i < 20; i++) reqs.add(10000L + offset + i);
                List<Long> allowed = new ArrayList<>();
                for (long t : reqs) if (lim.allow("c", t)) allowed.add(t);
                int w = worstSecond(allowed);
                lo = Math.min(lo, w); hi = Math.max(hi, w);
            }
            String nm = which == 0 ? "fixed window" : which == 1 ? "sliding window log"
                                                   : "token bucket 20/10";
            System.out.printf("  %-20s %14d %14d   %s%n", nm, lo, hi,
                    lo == hi ? "same every time" : "depends on timing");
        }
        System.out.println();

        // 3. a client that was quiet and then sends everything at once
        List<Long> saved = new ArrayList<>();
        for (int i = 0; i < 30; i++) saved.add(5000L + i);
        run("quiet for five seconds, then 30 at once", saved,
            "Nothing sent for five seconds, then 30 requests in 30 ms. The bucket holds 20.");
    }
}
