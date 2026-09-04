/**
 * Everything a payment method has to be able to do, and nothing more.
 * A new method is a new file that implements this. No existing file learns
 * about it.
 */
public interface PaymentMethod {
    String name();
    void validate(Order order);
    long feePaise(long amountPaise);
    String charge(Order order);
}
