import java.util.ArrayList;
import java.util.List;

/**
 * The same test against the clean version. Nothing real is connected to,
 * because CheckoutService only ever named interfaces.
 *
 *   java CleanTest.java
 */
public class CleanTest {

    static class FakeRepository implements OrderRepository {
        String orderId; String method; long feePaise;
        public void savePayment(String orderId, String method, long feePaise) {
            this.orderId = orderId; this.method = method; this.feePaise = feePaise;
        }
    }

    static class FakeNotifier implements Notifier {
        List<String> sentTo = new ArrayList<>();
        public void orderConfirmed(Order order) { sentTo.add(order.customerEmail()); }
    }

    static class FakeAudit implements AuditLog {
        String reference;
        public void paymentTaken(String orderId, String method, String reference, long fee) {
            this.reference = reference;
        }
    }

    static int passed = 0, failed = 0;
    static void check(String what, Object actual, Object expected) {
        boolean ok = actual.equals(expected);
        if (ok) passed++; else failed++;
        System.out.println("    " + (ok ? "pass" : "FAIL") + "  " + what
                + " = " + actual + (ok ? "" : "  expected " + expected));
    }

    public static void main(String[] args) {
        System.out.println("=== testing the clean version ===");
        System.out.println();

        FakeRepository repo = new FakeRepository();
        FakeNotifier mail = new FakeNotifier();
        FakeAudit audit = new FakeAudit();

        // one warm up run first, so we time the work and not class loading.
        // it gets its own fakes so it cannot pollute what we are about to check.
        new CheckoutService(List.of(new CardPayment(), new NetBankingPayment()),
                new FakeRepository(), new FakeNotifier(), new FakeAudit())
                .checkout(new Order("WARMUP", 250000, "warmup@example.com"), "card");

        CheckoutService service = new CheckoutService(
                List.of(new CardPayment(), new NetBankingPayment()), repo, mail, audit);

        System.out.println();
        System.out.println("  the run being timed:");
        long start = System.nanoTime();
        service.checkout(new Order("T-1", 250000, "test@example.com"), "card");
        long micros = (System.nanoTime() - start) / 1000;

        System.out.println();
        System.out.println("  what this test could check:");
        check("fee charged", repo.feePaise, 5175L);
        check("payment method recorded", repo.method, "card");
        check("order id recorded", repo.orderId, "T-1");
        check("email address used", mail.sentTo, List.of("test@example.com"));
        check("gateway reference", audit.reference, "CARD-T-1-ok");

        System.out.println();
        System.out.println("  " + passed + " passed, " + failed + " failed");
        System.out.println("  took " + micros + " microseconds");
        System.out.println("  no database, no mail server, nothing to clean up afterwards");
    }
}
