package demo.scope;

import org.springframework.stereotype.Component;

/**
 * The trap. A prototype injected into a singleton is resolved once, when the
 * singleton is built. After that the singleton holds one instance for ever, so
 * the prototype scope may as well not be there.
 */
@Component
public class SingletonHoldingPrototype {

    private final Prototype injectedOnce;

    public SingletonHoldingPrototype(Prototype injectedOnce) {
        this.injectedOnce = injectedOnce;
    }

    public int serialOfHeldPrototype() { return injectedOnce.serial(); }
}
