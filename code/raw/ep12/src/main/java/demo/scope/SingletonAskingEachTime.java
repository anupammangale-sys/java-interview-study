package demo.scope;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * The fix. Ask for the prototype at the moment it is needed instead of holding
 * one. ObjectProvider is a handle to the factory, not to an instance.
 */
@Component
public class SingletonAskingEachTime {

    private final ObjectProvider<Prototype> provider;

    public SingletonAskingEachTime(ObjectProvider<Prototype> provider) {
        this.provider = provider;
    }

    public int serialOfFreshPrototype() { return provider.getObject().serial(); }
}
