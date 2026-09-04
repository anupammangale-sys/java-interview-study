import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Two things that look similar and are not:
 *   reduce, which combines values by making a new one each time
 *   collect, which pours values into a container that is changed as it goes
 *
 * For numbers there is no difference. For anything you build up, there is.
 *
 *   java ReduceVsCollect.java
 */
public class ReduceVsCollect {

    public static void main(String[] args) {
        stringBuilding();
        System.out.println();
        collectorsTour();
    }

    private static void stringBuilding() {
        System.out.println("=== joining strings: reduce against collect ===");
        System.out.printf("%10s %14s %14s   %s%n", "items", "reduce ms", "collect ms", "ratio");
        System.out.println("-".repeat(60));

        for (int n : new int[]{1_000, 5_000, 20_000, 50_000}) {
            List<String> words = IntStream.range(0, n)
                    .mapToObj(i -> "w" + i)
                    .toList();

            long r = time(() -> words.stream().reduce("", (a, b) -> a + b).length());
            long c = time(() -> words.stream().collect(Collectors.joining()).length());

            System.out.printf("%,10d %,14d %,14d   %.0fx%n",
                    n, r / 1_000_000, c / 1_000_000, (double) r / Math.max(1, c));
        }
        System.out.println();
        System.out.println("reduce with + makes a whole new String every step, so the work grows");
        System.out.println("with the square of the item count. collect appends into one buffer.");
    }

    private static void collectorsTour() {
        System.out.println("=== what the common collectors actually return ===");
        record Person(String name, String dept, int salary) {}

        List<Person> people = List.of(
                new Person("Ann", "eng", 120),
                new Person("Bob", "eng", 100),
                new Person("Cat", "sales", 90),
                new Person("Dan", "sales", 95),
                new Person("Eve", "hr", 80));

        Map<String, List<Person>> byDept = people.stream()
                .collect(Collectors.groupingBy(Person::dept));
        System.out.println("  groupingBy(dept).size()          = " + byDept.size()
                + "  keys " + byDept.keySet());

        Map<String, Long> countByDept = people.stream()
                .collect(Collectors.groupingBy(Person::dept, Collectors.counting()));
        System.out.println("  groupingBy + counting            = " + countByDept);

        Map<String, List<String>> namesByDept = people.stream()
                .collect(Collectors.groupingBy(Person::dept,
                        Collectors.mapping(Person::name, Collectors.toList())));
        System.out.println("  groupingBy + mapping(name)       = " + namesByDept);

        Map<Boolean, List<String>> split = people.stream()
                .collect(Collectors.partitioningBy(p -> p.salary() >= 100,
                        Collectors.mapping(Person::name, Collectors.toList())));
        System.out.println("  partitioningBy(salary >= 100)    = " + split);

        Map<String, Integer> topPerDept = people.stream()
                .collect(Collectors.toMap(Person::dept, Person::salary, Math::max));
        System.out.println("  toMap with a merge function      = " + topPerDept);

        String joined = people.stream().map(Person::name)
                .collect(Collectors.joining(", ", "[", "]"));
        System.out.println("  joining with prefix and suffix   = " + joined);

        // teeing: two collectors at once, combined
        String summary = people.stream().collect(Collectors.teeing(
                Collectors.counting(),
                Collectors.averagingInt(Person::salary),
                (count, avg) -> count + " people, average " + avg));
        System.out.println("  teeing(count, average)           = " + summary);

        System.out.println();
        System.out.println("  partitioningBy ALWAYS has both keys, even when one side is empty:");
        Map<Boolean, List<Person>> nobody = people.stream()
                .collect(Collectors.partitioningBy(p -> p.salary() > 1000));
        System.out.println("    " + nobody);
        System.out.println("  groupingBy would simply have no key at all for a missing group.");
    }

    private static long time(Runnable r) {
        r.run();                    // warm it once
        long t0 = System.nanoTime();
        r.run();
        return System.nanoTime() - t0;
    }
}
