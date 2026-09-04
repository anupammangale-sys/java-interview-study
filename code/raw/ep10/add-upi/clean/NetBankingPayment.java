public class NetBankingPayment implements PaymentMethod {
    public String name() { return "netbanking"; }

    public void validate(Order order) {
        if (order.amountPaise() < 100) {
            throw new IllegalArgumentException("netbanking needs at least Rs 1");
        }
    }

    public long feePaise(long amountPaise) { return 1200; }

    public String charge(Order order) {
        System.out.println("  [bank portal] debited " + Money.rupees(order.amountPaise()));
        return "NB-" + order.id() + "-ok";
    }
}
