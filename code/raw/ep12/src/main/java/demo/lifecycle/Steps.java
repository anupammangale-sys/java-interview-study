package demo.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Records the order things actually happened in, rather than the order people remember. */
public class Steps {
    private static final AtomicInteger n = new AtomicInteger();
    private static final List<String> steps = new ArrayList<>();

    public static void record(String what) {
        steps.add(String.format("%2d. %s", n.incrementAndGet(), what));
    }
    public static List<String> all() { return List.copyOf(steps); }
    public static void print() { steps.forEach(s -> System.out.println("  " + s)); }
}
