import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

/**
 * Flattening nested things, and the checked exception problem that stops
 * every stream coding round sooner or later.
 *
 *   java FlattenAndExceptions.java
 */
public class FlattenAndExceptions {

    record Item(String sku, int qty) {}
    record Order(String id, List<Item> items) {}

    public static void main(String[] args) {
        flattening();
        System.out.println();
        checkedExceptions();
    }

    private static void flattening() {
        System.out.println("=== flattening ===");

        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3, 4, 5), List.of(), List.of(6));
        line("1. list of lists into one list");
        show(nested.stream().flatMap(List::stream).toList());

        line("   why map does not work: it gives you a stream of streams");
        show(nested.stream().map(List::stream).toList().size() + " Stream objects, not the numbers");

        Map<String, List<String>> byDept = new LinkedHashMap<>();
        byDept.put("eng", List.of("Ann", "Bob"));
        byDept.put("hr", List.of("Cara"));
        line("2. all values from a map of lists");
        show(byDept.values().stream().flatMap(List::stream).toList());

        List<Order> orders = List.of(
                new Order("o1", List.of(new Item("a", 2), new Item("b", 1))),
                new Order("o2", List.of(new Item("a", 5))),
                new Order("o3", List.of()));

        line("3. every item across every order");
        show(orders.stream().flatMap(o -> o.items().stream()).map(Item::sku).toList());

        line("4. total quantity per sku, across all orders");
        show(orders.stream()
                .flatMap(o -> o.items().stream())
                .collect(groupingBy(Item::sku, TreeMap::new, summingInt(Item::qty))));

        line("5. split sentences into words");
        List<String> sentences = List.of("the cat sat", "on the mat");
        show(sentences.stream().flatMap(s -> Arrays.stream(s.split(" "))).distinct().sorted().toList());

        line("6. keep only the orders that have items");
        show(orders.stream().filter(o -> !o.items().isEmpty()).map(Order::id).toList());

        line("7. flatMap with Optional: drop the empties");
        List<Optional<String>> maybes = List.of(Optional.of("x"), Optional.empty(), Optional.of("y"));
        show(maybes.stream().flatMap(Optional::stream).toList());
    }

    // ---- checked exceptions ----

    /** Stands in for anything that throws a checked exception. */
    private static int parse(String s) throws Exception {
        if (!s.matches("-?\\d+")) throw new Exception("not a number: " + s);
        return Integer.parseInt(s);
    }

    /** A functional interface that IS allowed to throw. */
    @FunctionalInterface
    interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /** Wraps a throwing function into an ordinary one. */
    private static <T, R> Function<T, R> unchecked(ThrowingFunction<T, R> f) {
        return t -> {
            try {
                return f.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static void checkedExceptions() {
        System.out.println("=== checked exceptions inside a lambda ===");
        List<String> good = List.of("1", "2", "3");
        List<String> mixed = List.of("1", "oops", "3");

        line("the problem: map(this::parse) does not compile, because Function");
        line("cannot throw a checked exception. Three ways round it.");
        System.out.println();

        line("A. try/catch inside the lambda, turning it into something unchecked");
        show(good.stream().map(s -> {
            try {
                return parse(s);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList());

        line("B. the same thing extracted into a helper, so the pipeline stays readable");
        show(good.stream().map(unchecked(FlattenAndExceptions::parse)).toList());

        line("C. do not throw at all: keep the failures and the successes");
        Map<Boolean, List<String>> split = mixed.stream()
                .collect(partitioningBy(FlattenAndExceptions::isNumber));
        show("valid " + split.get(true) + ", invalid " + split.get(false));

        line("   then parse only the valid ones");
        show(split.get(true).stream().map(Integer::parseInt).toList());

        line("what happens if you let B hit a bad value");
        try {
            mixed.stream().map(unchecked(FlattenAndExceptions::parse)).toList();
        } catch (RuntimeException e) {
            show(e.getClass().getSimpleName() + " wrapping " + e.getCause().getMessage());
        }
    }

    private static boolean isNumber(String s) {
        return s.matches("-?\\d+");
    }

    private static void line(String s) { System.out.println(s); }
    private static void show(Object o) { System.out.println("   -> " + o); System.out.println(); }
}
