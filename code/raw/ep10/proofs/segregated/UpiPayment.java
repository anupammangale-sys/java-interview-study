/** UPI can take money and give it back. It says nothing about the rest. */
public class UpiPayment implements PaymentMethod, Refundable {
    public String name() { return "upi"; }
    public String charge(long a) { return "collected"; }
    public String refund(long a) { return "refunded"; }
}
