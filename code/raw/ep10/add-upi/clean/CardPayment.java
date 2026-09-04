public class CardPayment implements PaymentMethod {
    public String name() { return "card"; }

    public void validate(Order order) {
        if (order.amountPaise() > 20000000) {
            throw new IllegalArgumentException("card limit is Rs 200000");
        }
    }

    public long feePaise(long amountPaise) {
        return Math.round(amountPaise * 0.0195) + 300;
    }

    public String charge(Order order) {
        System.out.println("  [card gateway] charged " + Money.rupees(order.amountPaise()));
        return "CARD-" + order.id() + "-ok";
    }
}
