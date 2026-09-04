/** What every payment method can do. Nothing else belongs here. */
public interface PaymentMethod {
    String name();
    String charge(long amountPaise);
}
