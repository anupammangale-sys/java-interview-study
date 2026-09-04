package demo.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PriceService {

    private final AtomicInteger realLookups = new AtomicInteger();

    /** Pretend this asks a slow service. The sleep is the work being avoided. */
    @Cacheable("prices")
    public long priceOf(String sku) {
        realLookups.incrementAndGet();
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return sku.length() * 100L;
    }

    /** The self call problem again: this one goes through the proxy, so it works. */
    @CacheEvict(value = "prices", key = "#sku")
    public void forget(String sku) {}

    public int realLookups() { return realLookups.get(); }
}
