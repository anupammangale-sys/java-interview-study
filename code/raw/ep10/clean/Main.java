import java.util.List;

/**
 * The only file that knows which real classes are in use. Everything else
 * names interfaces. This is the one place a new payment method is wired in,
 * and it is what a framework like Spring does for you.
 */
public class Main {
    public static void main(String[] args) {
        CheckoutService service = new CheckoutService(
                List.of(new CardPayment(), new NetBankingPayment()),
                new PostgresOrderRepository("jdbc:postgresql://prod-db:5432/orders"),
                new EmailNotifier("smtp.company.internal"),
                new ConsoleAuditLog());

        service.checkout(new Order("A-1001", 250000, "asha@example.com"), "card");
        System.out.println();
        service.checkout(new Order("A-1002", 899900, "ravi@example.com"), "netbanking");
    }
}
