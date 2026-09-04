import java.util.List;

/** Only the methods that really can be split are offered to the feature. */
public class Good {
    public static void main(String[] args) {
        System.out.println("split the bill, given only things that can be split:");
        for (Splittable s : List.<Splittable>of(new CardPayment())) {
            System.out.println("  " + SplitTheBill.run(s, 3, 50000));
        }
        System.out.println();
        System.out.println("everything can still be charged:");
        for (PaymentMethod m : List.of(new CardPayment(), new UpiPayment(), new CashOnDelivery())) {
            System.out.println("  " + m.name() + ": " + m.charge(50000));
        }
        System.out.println();
        System.out.println("methods written that exist only to throw: 0");
    }
}
