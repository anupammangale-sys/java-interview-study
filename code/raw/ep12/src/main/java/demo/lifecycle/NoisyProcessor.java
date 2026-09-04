package demo.lifecycle;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Runs for every bean in the context. This is the hook people forget exists,
 * and it is how @Transactional and @Cacheable get their proxies in: the
 * "after initialization" step is where Spring swaps your object for a wrapper.
 */
@Component
public class NoisyProcessor implements BeanPostProcessor {

    @Override public Object postProcessBeforeInitialization(Object bean, String name) {
        if (bean instanceof Noisy) Steps.record("BeanPostProcessor.postProcessBeforeInitialization");
        return bean;
    }

    @Override public Object postProcessAfterInitialization(Object bean, String name) {
        if (bean instanceof Noisy) Steps.record("BeanPostProcessor.postProcessAfterInitialization");
        return bean;
    }
}
