import java.util.List;

/**
 * Interface segregation, counted rather than argued.
 *
 * One interface grows until it covers everything any payment method might do.
 * Every method that a given class cannot honestly do still has to be written,
 * and the only thing it can do is throw. This program calls every method on
 * every class and counts how many exist purely to fail.
 *
 *   java FatInterfaceProof.java
 */
public class FatInterfaceProof {

    /** The interface after four features were added to it over two years. */
    interface PaymentMethod {
        String name();
        String charge(long amountPaise);
        String refund(long amountPaise);
        String schedule(long amountPaise, String date);
        String splitAcross(int people, long amountPaise);
    }

    static class CardPayment implements PaymentMethod {
        public String name() { return "card"; }
        public String charge(long a) { return "charged"; }
        public String refund(long a) { return "refunded"; }
        public String schedule(long a, String d) { return "scheduled for " + d; }
        public String splitAcross(int p, long a) { return "split across " + p; }
    }

    static class UpiPayment implements PaymentMethod {
        public String name() { return "upi"; }
        public String charge(long a) { return "collected"; }
        public String refund(long a) { return "refunded"; }
        public String schedule(long a, String d) {
            throw new UnsupportedOperationException("upi cannot schedule a future payment");
        }
        public String splitAcross(int p, long a) {
            throw new UnsupportedOperationException("upi cannot split one order across payers");
        }
    }

    static class CashOnDelivery implements PaymentMethod {
        public String name() { return "cash on delivery"; }
        public String charge(long a) { return "collected at the door"; }
        public String refund(long a) {
            throw new UnsupportedOperationException("cash refunds are handled by the courier");
        }
        public String schedule(long a, String d) {
            throw new UnsupportedOperationException("cash cannot be scheduled");
        }
        public String splitAcross(int p, long a) {
            throw new UnsupportedOperationException("cash cannot be split");
        }
    }

    interface Call { String run(PaymentMethod m); }

    public static void main(String[] args) {
        List<PaymentMethod> all = List.of(new CardPayment(), new UpiPayment(), new CashOnDelivery());
        List<String> names = List.of("charge", "refund", "schedule", "splitAcross");
        List<Call> calls = List.of(
                m -> m.charge(50000),
                m -> m.refund(50000),
                m -> m.schedule(50000, "2026-10-01"),
                m -> m.splitAcross(3, 50000));

        System.out.println("=== every method, called on every class ===");
        System.out.printf("%-18s%-12s%-12s%-12s%-12s%n",
                "", names.get(0), names.get(1), names.get(2), names.get(3));

        int works = 0, throwsOnly = 0;
        for (PaymentMethod m : all) {
            StringBuilder row = new StringBuilder(String.format("%-18s", m.name()));
            for (Call c : calls) {
                try {
                    c.run(m);
                    row.append(String.format("%-12s", "works"));
                    works++;
                } catch (UnsupportedOperationException e) {
                    row.append(String.format("%-12s", "throws"));
                    throwsOnly++;
                }
            }
            System.out.println(row);
        }

        int total = works + throwsOnly;
        System.out.println();
        System.out.println("  " + total + " method implementations had to be written.");
        System.out.println("  " + works + " do something. " + throwsOnly
                + " exist only to throw, which is " + (throwsOnly * 100 / total) + " percent.");
        System.out.println();
        System.out.println("=== what that costs at run time ===");
        System.out.println("  a 'split the bill' feature loops over the methods it was given:");
        for (PaymentMethod m : all) {
            try {
                System.out.println("    " + m.name() + ": " + m.splitAcross(3, 50000));
            } catch (UnsupportedOperationException e) {
                System.out.println("    " + m.name() + ": CRASH at run time, "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        System.out.println();
        System.out.println("  The compiler was happy with all of this. It had to be, because");
        System.out.println("  every class does implement every method. The type system was");
        System.out.println("  told they are all the same thing, so it cannot help.");
    }
}
