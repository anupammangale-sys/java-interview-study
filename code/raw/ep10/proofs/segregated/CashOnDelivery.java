/** Cash can only be collected. One interface, two methods, nothing that throws. */
public class CashOnDelivery implements PaymentMethod {
    public String name() { return "cash on delivery"; }
    public String charge(long a) { return "collected at the door"; }
}
