package demo.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** A separate bean, so calls to it really do go through a proxy. */
@Service
public class InnerService {

    private final OrderRepo repo;

    public InnerService(OrderRepo repo) { this.repo = repo; }

    /** Joins whatever transaction the caller already has. The default. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveThenFailRequired(String id) {
        repo.save(new OrderRow(id, "inner, REQUIRED"));
        throw new IllegalStateException("inner failed");
    }

    /** Suspends the caller's transaction and runs in one of its own. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveThenFailRequiresNew(String id) {
        repo.save(new OrderRow(id, "inner, REQUIRES_NEW"));
        throw new IllegalStateException("inner failed");
    }
}
