import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * The claim everyone repeats is that LinkedList is better for inserting and
 * removing. This measures it instead of repeating it.
 *
 * This is a rough measurement, not a rigorous benchmark: warmup rounds, then
 * the best of several timed rounds. Good enough to show differences of ten
 * times or more, which is what we are looking for. Do not read anything into
 * differences of a few percent.
 *
 *   java ListBenchmark.java
 */
public class ListBenchmark {

    private static final int SIZE = 100_000;
    private static final int OPS = 20_000;
    private static final int WARMUP = 5;
    private static final int ROUNDS = 7;

    public static void main(String[] args) {
        System.out.printf("%d elements, %d operations each, best of %d rounds%n%n", SIZE, OPS, ROUNDS);
        System.out.printf("%-28s %14s %14s   %s%n", "operation", "ArrayList", "LinkedList", "verdict");
        System.out.println("-".repeat(80));

        row("get by random index",     ListBenchmark::randomGet);
        row("add at the end",          ListBenchmark::addLast);
        row("add at the front",        ListBenchmark::addFirst);
        row("insert in the middle",    ListBenchmark::insertMiddle);
        row("walk the whole list",     ListBenchmark::iterate);
        row("remove while iterating",  ListBenchmark::removeViaIterator);
    }

    private static void row(String name, java.util.function.ToLongFunction<List<Integer>> work) {
        long a = best(work, false);
        long l = best(work, true);
        String verdict;
        double ratio = (double) Math.max(a, l) / Math.max(1, Math.min(a, l));
        if (ratio < 1.5) verdict = "about the same";
        else if (a < l) verdict = String.format("ArrayList %.0fx faster", ratio);
        else verdict = String.format("LinkedList %.0fx faster", ratio);
        System.out.printf("%-28s %11d us %11d us   %s%n", name, a / 1000, l / 1000, verdict);
    }

    private static long best(java.util.function.ToLongFunction<List<Integer>> work, boolean linked) {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < WARMUP + ROUNDS; i++) {
            List<Integer> list = linked ? new LinkedList<>() : new ArrayList<>();
            for (int j = 0; j < SIZE; j++) list.add(j);
            long ns = work.applyAsLong(list);
            if (i >= WARMUP) best = Math.min(best, ns);
        }
        return best;
    }

    // ---- the operations ----

    private static long randomGet(List<Integer> list) {
        Random r = new Random(42);
        int[] idx = new int[OPS];
        for (int i = 0; i < OPS; i++) idx[i] = r.nextInt(SIZE);
        long start = System.nanoTime();
        long sum = 0;
        for (int i : idx) sum += list.get(i);
        long ns = System.nanoTime() - start;
        if (sum == -1) System.out.print("");
        return ns;
    }

    private static long addLast(List<Integer> list) {
        long start = System.nanoTime();
        for (int i = 0; i < OPS; i++) list.add(i);
        return System.nanoTime() - start;
    }

    private static long addFirst(List<Integer> list) {
        long start = System.nanoTime();
        for (int i = 0; i < OPS; i++) list.add(0, i);
        return System.nanoTime() - start;
    }

    private static long insertMiddle(List<Integer> list) {
        long start = System.nanoTime();
        for (int i = 0; i < OPS; i++) list.add(list.size() / 2, i);
        return System.nanoTime() - start;
    }

    private static long iterate(List<Integer> list) {
        long start = System.nanoTime();
        long sum = 0;
        for (int v : list) sum += v;
        long ns = System.nanoTime() - start;
        if (sum == -1) System.out.print("");
        return ns;
    }

    private static long removeViaIterator(List<Integer> list) {
        long start = System.nanoTime();
        Iterator<Integer> it = list.iterator();
        int n = 0;
        while (it.hasNext()) {
            it.next();
            if (n++ % 2 == 0) it.remove();
        }
        return System.nanoTime() - start;
    }
}
