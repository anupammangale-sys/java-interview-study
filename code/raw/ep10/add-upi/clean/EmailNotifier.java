public class EmailNotifier implements Notifier {
    public EmailNotifier(String host) {
        System.out.println("  [smtp] opening connection to " + host);
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    public void orderConfirmed(Order order) {
        System.out.println("  [smtp] to " + order.customerEmail()
                + " subject: Order " + order.id() + " confirmed");
        System.out.println("  [smtp] body: Thank you. We received "
                + Money.rupees(order.amountPaise()) + ".");
    }
}
