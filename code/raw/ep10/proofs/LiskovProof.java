import java.util.List;

/**
 * Liskov substitution, proved rather than recited.
 *
 * A parent class makes a promise. A subclass is allowed to keep that promise
 * differently. It is not allowed to break it. Here one subclass breaks it, and
 * the damage is not a neat exception at the top of the program: the batch stops
 * halfway, after real money has already moved.
 *
 *   java LiskovProof.java
 */
public class LiskovProof {

    static String rupees(long paise) {
        return String.format("Rs %d.%02d", paise / 100, Math.abs(paise % 100));
    }

    /**
     * The promise: call refund with an amount, get that amount back to the
     * customer. Every subclass is expected to honour this.
     */
    static abstract class Refundable {
        private final String label;
        String labelOverride;
        Refundable(String label) { this.label = label; }
        String label() { return labelOverride != null ? labelOverride : label; }
        abstract long refund(long amountPaise);
    }

    static class CardRefund extends Refundable {
        CardRefund() { super("card"); }
        long refund(long amountPaise) { return amountPaise; }
    }

    static class NetBankingRefund extends Refundable {
        NetBankingRefund() { super("netbanking"); }
        long refund(long amountPaise) { return amountPaise; }
    }

    /**
     * Written by someone reasonable. A gift card behaves like a card in almost
     * every way, so extending it looked like a saving. But the business rule is
     * that gift card money is never returned, so refund cannot honour the promise.
     */
    static class GiftCardRefund extends CardRefund {
        GiftCardRefund() { this.labelOverride = "gift card"; }
        @Override long refund(long amountPaise) {
            throw new UnsupportedOperationException("gift cards are not refundable");
        }
    }

    /** One test, written once, against the promise the parent made. */
    static boolean contractHolds(Refundable r) {
        try {
            return r.refund(50000) == 50000;
        } catch (RuntimeException e) {
            System.out.println("    " + r.label() + ": " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
            return false;
        }
    }

    /** Real code that was written against the parent type, not against any subclass. */
    static void runRefundBatch(List<Refundable> batch, long each) {
        long returned = 0;
        int done = 0;
        try {
            for (Refundable r : batch) {
                returned += r.refund(each);
                done++;
                System.out.println("    refunded " + rupees(each) + " by " + r.label());
            }
            System.out.println("  batch finished, " + done + " of " + batch.size()
                    + ", total " + rupees(returned));
        } catch (RuntimeException e) {
            System.out.println("    STOPPED: " + e.getMessage());
            System.out.println("  batch died after " + done + " of " + batch.size()
                    + ". " + rupees(returned) + " already left the account and the rest");
            System.out.println("  never ran. Nobody wrote a bug. The subclass broke a promise.");
        }
    }

    public static void main(String[] args) {
        System.out.println("=== the same test, run against three subclasses ===");
        for (Refundable r : List.of(new CardRefund(), new NetBankingRefund(), new GiftCardRefund())) {
            System.out.printf("  %-12s contract holds? %s%n", r.label(), contractHolds(r));
        }

        System.out.println();
        System.out.println("=== a refund batch, written against the parent type ===");
        System.out.println("  all subclasses keeping the promise:");
        runRefundBatch(List.of(new CardRefund(), new NetBankingRefund(), new CardRefund()), 50000);

        System.out.println();
        System.out.println("  one gift card in the middle:");
        runRefundBatch(List.of(new CardRefund(), new GiftCardRefund(), new CardRefund()), 50000);
    }
}
