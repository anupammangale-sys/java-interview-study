package demo.scope;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/** The default scope. One instance for the whole application. */
@Component
public class SingletonBean {
    private static final AtomicInteger created = new AtomicInteger();
    private final int serial = created.incrementAndGet();
    public int serial() { return serial; }
}
