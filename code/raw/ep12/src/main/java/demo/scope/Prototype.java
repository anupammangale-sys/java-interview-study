package demo.scope;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/** A new one every time it is asked for. In theory. */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Prototype {
    private static final AtomicInteger created = new AtomicInteger();
    private final int serial = created.incrementAndGet();

    public int serial() { return serial; }
    public static int totalCreated() { return created.get(); }
}
