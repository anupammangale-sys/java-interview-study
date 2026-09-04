import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates a checkout and does none of the work itself. It names four
 * interfaces and not one concrete class, so every part of it can be swapped
 * or faked without editing this file.
 */
public class CheckoutService {

    private final Map<String, PaymentMethod> methods = new LinkedHashMap<>();
    private final OrderRepository repository;
    private final Notifier notifier;
    private final AuditLog audit;

    public CheckoutService(List<PaymentMethod> supported,
                           OrderRepository repository,
                           Notifier notifier,
                           AuditLog audit) {
        for (PaymentMethod m : supported) this.methods.put(m.name(), m);
        this.repository = repository;
        this.notifier = notifier;
        this.audit = audit;
    }

    public void checkout(Order order, String methodName) {
        System.out.println("checkout " + order.id() + " for " + Money.rupees(order.amountPaise())
                + " by " + methodName);

        if (order.amountPaise() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        PaymentMethod method = methods.get(methodName);
        if (method == null) {
            throw new IllegalArgumentException("unknown payment method: " + methodName);
        }

        method.validate(order);
        long fee = method.feePaise(order.amountPaise());
        String reference = method.charge(order);

        repository.savePayment(order.id(), methodName, fee);
        notifier.orderConfirmed(order);
        audit.paymentTaken(order.id(), methodName, reference, fee);
    }
}
