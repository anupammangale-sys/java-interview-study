/**
 * One class that does the whole checkout. This is not a strawman. It is what
 * the code looks like when a working feature is shipped under a deadline and
 * every later change is added to the method that was already there.
 */
public class OrderService {

    public void checkout(Order order, String method) {
        System.out.println("checkout " + order.id() + " for " + Money.rupees(order.amountPaise())
                + " by " + method);

        // 1. validation
        if (order.amountPaise() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (method.equals("card")) {
            if (order.amountPaise() > 20000000) {
                throw new IllegalArgumentException("card limit is Rs 200000");
            }
        } else if (method.equals("netbanking")) {
            if (order.amountPaise() < 100) {
                throw new IllegalArgumentException("netbanking needs at least Rs 1");
            }
        } else if (method.equals("upi")) {
            if (order.amountPaise() > 10000000) {
                throw new IllegalArgumentException("upi limit is Rs 100000");
            }
        } else {
            throw new IllegalArgumentException("unknown payment method: " + method);
        }

        // 2. what the payment provider charges us
        long fee;
        if (method.equals("card")) {
            fee = Math.round(order.amountPaise() * 0.0195) + 300;
        } else if (method.equals("netbanking")) {
            fee = 1200;
        } else if (method.equals("upi")) {
            fee = 0;
        } else {
            throw new IllegalArgumentException("unknown payment method: " + method);
        }

        // 3. actually take the money
        String reference;
        if (method.equals("card")) {
            reference = "CARD-" + order.id() + "-ok";
            System.out.println("  [card gateway] charged " + Money.rupees(order.amountPaise()));
        } else if (method.equals("netbanking")) {
            reference = "NB-" + order.id() + "-ok";
            System.out.println("  [bank portal] debited " + Money.rupees(order.amountPaise()));
        } else if (method.equals("upi")) {
            reference = "UPI-" + order.id() + "-ok";
            System.out.println("  [upi handle] collected " + Money.rupees(order.amountPaise()));
        } else {
            throw new IllegalArgumentException("unknown payment method: " + method);
        }

        // 4. store it
        Database db = Database.connect("jdbc:postgresql://prod-db:5432/orders");
        db.savePayment(order.id(), method, fee);

        // 5. tell the customer
        Smtp smtp = Smtp.connect("smtp.company.internal");
        smtp.send(order.customerEmail(),
                "Order " + order.id() + " confirmed",
                "Thank you. We received " + Money.rupees(order.amountPaise()) + ".");

        // 6. audit trail
        System.out.println("  [audit] " + order.id() + " paid by " + method
                + " reference " + reference + " fee " + Money.rupees(fee));
    }
}
