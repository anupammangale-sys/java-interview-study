import java.util.concurrent.StructuredTaskScope;

/**
 * Structured concurrency: several tasks treated as one unit of work.
 *
 * Still a preview feature, so it needs the flag:
 *
 *   java --enable-preview --source 24 StructuredDemo.java
 *
 * The point is that if one subtask fails, the others are cancelled
 * automatically, and the scope cannot be left open by accident.
 */
public class StructuredDemo {

    record Order(String user, String cart) {}

    public static void main(String[] args) throws Exception {
        System.out.println("Two subtasks, both needed to build one result.");
        System.out.println();

        System.out.println("1. both succeed:");
        System.out.println("   " + fetch(false));

        System.out.println();
        System.out.println("2. one fails, so the other is cancelled:");
        try {
            System.out.println("   " + fetch(true));
        } catch (Exception e) {
            System.out.println("   whole scope failed: " + e.getCause().getMessage());
        }
    }

    private static Order fetch(boolean failCart) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var user = scope.fork(() -> slow("user-42", 100, false));
            var cart = scope.fork(() -> slow("cart-7", 300, failCart));

            scope.join();            // wait for both
            scope.throwIfFailed();   // rethrow the first failure, if any

            return new Order(user.get(), cart.get());
        }
    }

    private static String slow(String value, int ms, boolean fail) throws Exception {
        Thread.sleep(ms);
        if (fail) throw new IllegalStateException("cart service is down");
        return value;
    }
}
