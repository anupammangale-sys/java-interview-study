package demo.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "I caught the exception, so why did everything still roll back?"
 *
 * With REQUIRED the inner method joins the caller's transaction. When it
 * throws, Spring marks that one shared transaction rollback only. Catching
 * the exception does not unmark it, so the commit at the end fails with
 * UnexpectedRollbackException and the outer save is lost too.
 */
@Service
public class PropagationDemo {

    private final OrderRepo repo;
    private final InnerService inner;

    public PropagationDemo(OrderRepo repo, InnerService inner) {
        this.repo = repo;
        this.inner = inner;
    }

    @Transactional
    public void outerCatchesRequired(String outerId, String innerId) {
        repo.save(new OrderRow(outerId, "outer work"));
        try {
            inner.saveThenFailRequired(innerId);
        } catch (IllegalStateException e) {
            System.out.println("  outer caught: " + e.getMessage() + ", carrying on");
        }
    }

    @Transactional
    public void outerCatchesRequiresNew(String outerId, String innerId) {
        repo.save(new OrderRow(outerId, "outer work"));
        try {
            inner.saveThenFailRequiresNew(innerId);
        } catch (IllegalStateException e) {
            System.out.println("  outer caught: " + e.getMessage() + ", carrying on");
        }
    }
}
