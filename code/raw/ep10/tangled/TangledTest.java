/**
 * An honest attempt to unit test the tangled OrderService.
 *
 * There is nothing to hand in, so checkout() opens its own database
 * connection and its own mail connection. On a real machine those point at
 * a real server, so this is not a unit test at all.
 *
 *   java TangledTest.java
 */
public class TangledTest {
    public static void main(String[] args) {
        System.out.println("=== testing the tangled version ===");
        System.out.println();

        OrderService service = new OrderService();
        // one warm up run first, so we time the work and not class loading
        service.checkout(new Order("WARMUP", 250000, "test@example.com"), "card");

        System.out.println();
        System.out.println("  the run being timed:");
        long start = System.nanoTime();
        service.checkout(new Order("T-1", 250000, "test@example.com"), "card");
        long micros = (System.nanoTime() - start) / 1000;

        System.out.println();
        System.out.println("  took " + micros + " microseconds, which is "
                + (micros / 1000) + " ms");
        System.out.println();
        System.out.println("  what this test could check:");
        System.out.println("    1. checkout did not throw");
        System.out.println();
        System.out.println("  what it could NOT check:");
        System.out.println("    the fee, because the number went into the database and was");
        System.out.println("      never handed back");
        System.out.println("    the email, because it went to a mail server");
        System.out.println("    the audit line, because it went to standard output");
        System.out.println();
        System.out.println("  it also wrote a row into whatever database that url points at,");
        System.out.println("  and sent a real email, every time anyone ran the test suite.");
    }
}
