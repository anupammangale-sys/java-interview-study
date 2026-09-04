public class UpiPayment implements PaymentMethod {
    public String name() { return "upi"; }

    public void validate(Order order) {
        if (order.amountPaise() > 10000000) {
            throw new IllegalArgumentException("upi limit is Rs 100000");
        }
    }

    public long feePaise(long amountPaise) { return 0; }

    public String charge(Order order) {
        System.out.println("  [upi handle] collected " + Money.rupees(order.amountPaise()));
        return "UPI-" + order.id() + "-ok";
    }
}
