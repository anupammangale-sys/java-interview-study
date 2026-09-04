import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * What a stream actually does, observed rather than described.
 *
 *   java StreamLaziness.java
 */
public class StreamLaziness {

    public static void main(String[] args) {
        nothingHappens();
        System.out.println();
        oneAtATime();
        System.out.println();
        shortCircuit();
        System.out.println();
        consumedOnce();
    }

    /** No terminal operation means no work at all. */
    private static void nothingHappens() {
        System.out.println("=== 1. a pipeline with no terminal operation ===");
        AtomicInteger touched = new AtomicInteger();

        Stream<String> pipeline = Stream.of("a", "b", "c")
                .filter(s -> { touched.incrementAndGet(); return true; })
                .map(s -> { touched.incrementAndGet(); return s.toUpperCase(); });

        System.out.println("  pipeline built, elements touched: " + touched.get());
        pipeline.toList();                       // now add the terminal operation
        System.out.println("  after toList(),  elements touched: " + touched.get());
    }

    /** Elements go through the WHOLE pipeline one at a time, not stage by stage. */
    private static void oneAtATime() {
        System.out.println("=== 2. what order do the stages actually run in? ===");
        List<String> order = new java.util.ArrayList<>();

        List<String> out = Stream.of("ann", "bob", "cat")
                .peek(s -> order.add("filter " + s))
                .filter(s -> s.length() == 3)
                .peek(s -> order.add("  map " + s))
                .map(String::toUpperCase)
                .toList();

        System.out.println("  result: " + out);
        System.out.println("  order the stages ran:");
        order.forEach(s -> System.out.println("    " + s));
        System.out.println("  Not: filter all, then map all. Each element goes all the way through first.");
    }

    /** findFirst stops as soon as it has an answer. */
    private static void shortCircuit() {
        System.out.println("=== 3. how much of a million element stream gets looked at? ===");

        AtomicInteger filtered = new AtomicInteger();
        AtomicInteger mapped = new AtomicInteger();

        Optional<Integer> first = IntStream.rangeClosed(1, 1_000_000)
                .boxed()
                .filter(n -> { filtered.incrementAndGet(); return n % 7 == 0; })
                .map(n -> { mapped.incrementAndGet(); return n * 2; })
                .findFirst();

        System.out.printf("  findFirst() gave %s%n", first.orElse(-1));
        System.out.printf("  filter ran %d times, map ran %d time(s), out of 1,000,000%n",
                filtered.get(), mapped.get());

        AtomicInteger counted = new AtomicInteger();
        long total = IntStream.rangeClosed(1, 1_000_000)
                .boxed()
                .filter(n -> { counted.incrementAndGet(); return n % 7 == 0; })
                .count();
        System.out.printf("  count() gave %,d and had to look at %,d elements%n", total, counted.get());
    }

    /** A stream is not a collection: you get one pass. */
    private static void consumedOnce() {
        System.out.println("=== 4. reusing a stream ===");
        Stream<String> s = Stream.of("a", "b");
        System.out.println("  first  use: " + s.toList());
        try {
            System.out.println("  second use: " + s.toList());
        } catch (IllegalStateException e) {
            System.out.println("  second use: " + e.getClass().getSimpleName()
                    + " - " + e.getMessage());
        }
    }
}
