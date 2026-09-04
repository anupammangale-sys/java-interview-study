package demo.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The single most asked Spring transaction question: why does calling your own
 * @Transactional method from inside the same class do nothing?
 *
 * @Transactional works by wrapping the bean in a proxy. Spring hands everyone
 * else the proxy, not this object. A call to this.something() never leaves the
 * object, so the proxy is not involved, so no transaction is started.
 */
@Service
public class SelfCallDemo {

    private final OrderRepo repo;

    public SelfCallDemo(OrderRepo repo) { this.repo = repo; }

    /** Not transactional. Calls its own transactional method. */
    public void viaSelfCall(String id) {
        System.out.println("  outer: transaction active? "
                + TransactionSynchronizationManager.isActualTransactionActive());
        this.saveThenFail(id);          // never touches the proxy
    }

    /**
     * Reached two ways in this demo: from viaSelfCall above, and directly from
     * outside the bean. Same code, two completely different outcomes.
     */
    @Transactional
    public void saveThenFail(String id) {
        System.out.println("  inside saveThenFail: transaction active? "
                + TransactionSynchronizationManager.isActualTransactionActive());
        repo.save(new OrderRow(id, "should be rolled back"));
        throw new IllegalStateException("something went wrong after the save");
    }
}
