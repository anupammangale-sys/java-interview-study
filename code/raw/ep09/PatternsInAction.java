import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Strategy, Factory, Builder, Observer, Decorator and Adapter, each small
 * enough to read and each actually run.
 *
 *   java PatternsInAction.java
 */
public class PatternsInAction {

    public static void main(String[] args) {
        strategy();
        factory();
        builder();
        observer();
        decorator();
        adapter();
    }

    // ================= STRATEGY =================

    /** The version that grows a new branch every time the business changes. */
    static double feeIfElse(String method, double amount) {
        if (method.equals("card"))        return amount * 0.029 + 0.30;
        else if (method.equals("bank"))   return 0.25;
        else if (method.equals("wallet")) return amount * 0.015;
        else if (method.equals("crypto")) return amount * 0.01;
        else throw new IllegalArgumentException("unknown method: " + method);
    }

    /** The same rules, as a lookup. Adding one means adding one entry. */
    static final Map<String, Function<Double, Double>> FEES = Map.of(
            "card",   a -> a * 0.029 + 0.30,
            "bank",   a -> 0.25,
            "wallet", a -> a * 0.015,
            "crypto", a -> a * 0.01);

    static double feeStrategy(String method, double amount) {
        Function<Double, Double> f = FEES.get(method);
        if (f == null) throw new IllegalArgumentException("unknown method: " + method);
        return f.apply(amount);
    }

    /** When the set is fixed and known, an enum carries the behaviour itself. */
    enum Fee {
        CARD   { double of(double a) { return a * 0.029 + 0.30; } },
        BANK   { double of(double a) { return 0.25; } },
        WALLET { double of(double a) { return a * 0.015; } },
        CRYPTO { double of(double a) { return a * 0.01; } };
        abstract double of(double amount);
    }

    static void strategy() {
        head("STRATEGY: replacing an if-else chain");
        double amount = 100;
        for (String m : List.of("card", "bank", "wallet", "crypto")) {
            System.out.printf("  %-7s if-else %.2f   map %.2f   enum %.2f%n",
                    m, feeIfElse(m, amount), feeStrategy(m, amount),
                    Fee.valueOf(m.toUpperCase()).of(amount));
        }
        System.out.println("  all three agree. The difference is what happens when a fifth");
        System.out.println("  method arrives: the if-else needs editing, the map needs an entry.");
        System.out.println();
    }

    // ================= FACTORY =================

    interface Report { String render(); }
    record CsvReport(List<String> rows) implements Report {
        public String render() { return String.join(",", rows); }
    }
    record JsonReport(List<String> rows) implements Report {
        public String render() { return "[\"" + String.join("\",\"", rows) + "\"]"; }
    }

    static Report reportFor(String format, List<String> rows) {
        return switch (format) {
            case "csv"  -> new CsvReport(rows);
            case "json" -> new JsonReport(rows);
            default -> throw new IllegalArgumentException("no such format: " + format);
        };
    }

    static void factory() {
        head("FACTORY: the caller asks for what it wants, not how to build it");
        List<String> rows = List.of("ann", "bob");
        for (String f : List.of("csv", "json")) {
            System.out.println("  " + f + " -> " + reportFor(f, rows).render());
        }
        System.out.println("  the caller never names CsvReport or JsonReport.");
        System.out.println();
    }

    // ================= BUILDER =================

    static class Pizza {
        private final String size;
        private final List<String> toppings;
        private final boolean extraCheese;

        private Pizza(Builder b) {
            this.size = b.size;
            this.toppings = List.copyOf(b.toppings);
            this.extraCheese = b.extraCheese;
        }
        @Override public String toString() {
            return size + " pizza with " + toppings + (extraCheese ? " and extra cheese" : "");
        }
        static Builder of(String size) { return new Builder(size); }

        static class Builder {
            private final String size;
            private final List<String> toppings = new ArrayList<>();
            private boolean extraCheese;

            Builder(String size) { this.size = size; }
            Builder topping(String t) { toppings.add(t); return this; }
            Builder extraCheese() { extraCheese = true; return this; }
            Pizza build() {
                if (toppings.isEmpty()) throw new IllegalStateException("needs a topping");
                return new Pizza(this);
            }
        }
    }

    static void builder() {
        head("BUILDER: many optional parts, and the object is immutable when done");
        System.out.println("  " + Pizza.of("large").topping("ham").topping("olive").extraCheese().build());
        try {
            Pizza.of("small").build();
        } catch (IllegalStateException e) {
            System.out.println("  validation happens in build(): " + e.getMessage());
        }
        System.out.println();
    }

    // ================= OBSERVER =================

    interface Listener { void onEvent(String event); }

    static class Publisher {
        private final List<Listener> listeners = new ArrayList<>();
        void subscribe(Listener l) { listeners.add(l); }
        void unsubscribe(Listener l) { listeners.remove(l); }
        void publish(String event) {
            for (Listener l : List.copyOf(listeners)) l.onEvent(event);
        }
    }

    static void observer() {
        head("OBSERVER: one thing happens, several parties care");
        Publisher orders = new Publisher();
        Listener email = e -> System.out.println("    email service saw " + e);
        Listener audit = e -> System.out.println("    audit log saw " + e);
        orders.subscribe(email);
        orders.subscribe(audit);
        orders.publish("order-1 placed");
        orders.unsubscribe(email);
        System.out.println("  after the email service unsubscribes:");
        orders.publish("order-2 placed");
        System.out.println("  forgetting to unsubscribe is the leak from Episode 1.");
        System.out.println();
    }

    // ================= DECORATOR =================

    static void decorator() {
        head("DECORATOR: adding behaviour by wrapping, not by subclassing");
        UnaryOperator<String> plain = s -> s;
        UnaryOperator<String> trimmed = wrap(plain, String::trim);
        UnaryOperator<String> trimmedUpper = wrap(trimmed, String::toUpperCase);
        UnaryOperator<String> full = wrap(trimmedUpper, s -> "[" + s + "]");

        String input = "  hello  ";
        System.out.println("  plain        -> '" + plain.apply(input) + "'");
        System.out.println("  trimmed      -> '" + trimmed.apply(input) + "'");
        System.out.println("  + uppercase  -> '" + trimmedUpper.apply(input) + "'");
        System.out.println("  + brackets   -> '" + full.apply(input) + "'");
        System.out.println("  each layer wraps the one before. No subclass anywhere.");
        System.out.println();
    }

    static UnaryOperator<String> wrap(UnaryOperator<String> inner, UnaryOperator<String> extra) {
        return s -> extra.apply(inner.apply(s));
    }

    // ================= ADAPTER =================

    /** What our code wants to use. */
    interface Notifier { void notify(String who, String text); }

    /** What the third party library actually gives us. */
    static class LegacySms {
        void dispatch(String payload) { System.out.println("    LegacySms sent: " + payload); }
    }

    /** The adapter makes one look like the other. */
    record SmsAdapter(LegacySms sms) implements Notifier {
        public void notify(String who, String text) {
            sms.dispatch(who + "|" + text);
        }
    }

    static void adapter() {
        head("ADAPTER: making something you cannot change fit an interface you own");
        Notifier n = new SmsAdapter(new LegacySms());
        n.notify("+44123", "your order shipped");
        System.out.println("  our code only knows Notifier. LegacySms never changed.");
        System.out.println();
    }

    private static void head(String s) { System.out.println("=== " + s + " ==="); }
}
