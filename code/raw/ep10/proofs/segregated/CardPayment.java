public class CardPayment implements PaymentMethod, Refundable, Schedulable, Splittable {
    public String name() { return "card"; }
    public String charge(long a) { return "charged"; }
    public String refund(long a) { return "refunded"; }
    public String schedule(long a, String d) { return "scheduled for " + d; }
    public String splitAcross(int p, long a) { return "split across " + p; }
}
