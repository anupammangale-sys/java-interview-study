import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Does going parallel make it faster? Measured across several shapes of work,
 * because the answer changes completely depending on the shape.
 *
 * Rough measurement, not a rigorous benchmark: warmup rounds then best of
 * several. Good enough for differences of two times or more.
 *
 *   java ParallelStreams.java
 */
public class ParallelStreams {

    private static final int WARMUP = 3;
    private static final int ROUNDS = 5;

    public static void main(String[] args) {
        System.out.printf("processors available: %d%n", Runtime.getRuntime().availableProcessors());
        System.out.println("best of " + ROUNDS + " rounds after " + WARMUP + " warmups, times in microseconds");
        System.out.println();
        System.out.printf("%-46s %12s %12s   %s%n", "work", "sequential", "parallel", "verdict");
        System.out.println("-".repeat(94));

        // 1. tiny collection, trivial work
        List<Integer> small = range(1_000);
        compare("1,000 items, add them up",
                () -> small.stream().mapToLong(Integer::longValue).sum(),
                () -> small.parallelStream().mapToLong(Integer::longValue).sum());

        // 2. large collection, trivial work
        List<Integer> big = range(2_000_000);
        compare("2,000,000 items, add them up",
                () -> big.stream().mapToLong(Integer::longValue).sum(),
                () -> big.parallelStream().mapToLong(Integer::longValue).sum());

        // 3. large collection, expensive work per element
        List<Integer> medium = range(20_000);
        compare("20,000 items, real work on each",
                () -> medium.stream().mapToLong(ParallelStreams::expensive).sum(),
                () -> medium.parallelStream().mapToLong(ParallelStreams::expensive).sum());

        // 4. expensive work, but the source is a LinkedList
        List<Integer> linked = new LinkedList<>(medium);
        compare("20,000 in a LinkedList, real work on each",
                () -> linked.stream().mapToLong(ParallelStreams::expensive).sum(),
                () -> linked.parallelStream().mapToLong(ParallelStreams::expensive).sum());

        // 5. cheap work, where the cost of splitting the source dominates
        List<Integer> bigArray = range(1_000_000);
        List<Integer> bigLinked = new LinkedList<>(bigArray);
        compare("1,000,000 in an ArrayList, add them up",
                () -> bigArray.stream().mapToLong(Integer::longValue).sum(),
                () -> bigArray.parallelStream().mapToLong(Integer::longValue).sum());
        compare("1,000,000 in a LinkedList, add them up",
                () -> bigLinked.stream().mapToLong(Integer::longValue).sum(),
                () -> bigLinked.parallelStream().mapToLong(Integer::longValue).sum());

        // boxing, measured on its own terms rather than in the parallel columns
        System.out.println();
        System.out.println("Boxing cost, both sequential:");
        long boxed = best(() -> big.stream().mapToLong(Integer::longValue).sum());
        long prim = best(() -> IntStream.rangeClosed(1, 2_000_000).asLongStream().sum());
        System.out.printf("  2,000,000 boxed Integers  %,10d us%n", boxed / 1000);
        System.out.printf("  2,000,000 primitive ints  %,10d us   %.1fx faster%n",
                prim / 1000, (double) boxed / Math.max(1, prim));
    }

    private static List<Integer> range(int n) {
        List<Integer> list = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) list.add(i);
        return list;
    }

    /** Stands in for real per element work. */
    private static long expensive(int n) {
        long v = n;
        for (int i = 0; i < 400; i++) v = (v * 31 + i) % 1_000_003;
        return v;
    }

    private static void compare(String label, Supplier<Long> seq, Supplier<Long> par) {
        long s = best(seq), p = best(par);
        double ratio = (double) Math.max(s, p) / Math.max(1, Math.min(s, p));
        String verdict;
        if (ratio < 1.2) verdict = "about the same";
        else if (p < s) verdict = String.format("parallel %.1fx faster", ratio);
        else verdict = String.format("parallel %.1fx SLOWER", ratio);
        System.out.printf("%-46s %,12d %,12d   %s%n", label, s / 1000, p / 1000, verdict);
    }

    private static long best(Supplier<Long> work) {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < WARMUP + ROUNDS; i++) {
            long t0 = System.nanoTime();
            long result = work.get();
            long ns = System.nanoTime() - t0;
            if (result == Long.MIN_VALUE) System.out.print("");   // keep it alive
            if (i >= WARMUP) best = Math.min(best, ns);
        }
        return best;
    }
}
