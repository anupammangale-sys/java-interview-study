package demo;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * The same scenarios as the hand written version, through Resilience4j 2.2.0.
 *
 *   mvn compile
 *   mvn exec:java   (or run demo.Main with the built classpath)
 */
public class Main {

    // ---- the downstream service, same idea as the hand written demo ----
    static class Downstream {
        private final AtomicInteger received = new AtomicInteger();
        private volatile boolean healthy = true;
        private volatile int failPercent = 0;
        private volatile long latencyMs = 5;
        private final Random rnd;
        Downstream()          { this(7); }
        Downstream(long seed) { this.rnd = new Random(seed); }

        String call() {
            received.incrementAndGet();
            sleep(latencyMs);
            boolean fail = !healthy || (failPercent > 0 && rnd.nextInt(100) < failPercent);
            if (fail) throw new RuntimeException("downstream is unwell");
            return "ok";
        }
        void goDown()               { healthy = false; }
        void failSometimes(int pct) { healthy = true; failPercent = pct; }
        void slowTo(long ms)        { latencyMs = ms; }
        int received()              { return received.get(); }
    }

    /**
     * The hand written breaker again, cut down to the part that matters here:
     * it counts failures IN A ROW. That one design choice is what the second
     * demo below is about.
     */
    static class ConsecutiveBreaker {
        private final int threshold;
        private int inARow;
        private boolean open;
        private int refused;

        ConsecutiveBreaker(int threshold) { this.threshold = threshold; }
        synchronized boolean allow() { if (open) { refused++; return false; } return true; }
        synchronized void success()  { inARow = 0; }
        synchronized void failure()  { if (++inARow >= threshold) open = true; }
        synchronized boolean isOpen(){ return open; }
        synchronized int refused()   { return refused; }
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    static void head(String s) {
        System.out.println();
        System.out.println("=== " + s + " ===");
    }

    public static void main(String[] args) throws Exception {
        sameScenarioAsHandWritten();
        theCaseForTheLibrary();
        theOrderQuestion();
        bulkheadAndRateLimiter();
    }

    // ------------------------------------------------------------------ 1

    private static CircuitBreaker countBased(String name) {
        return countBased(name, 10, 5);
    }

    private static CircuitBreaker countBased(String name, int window, int minimum) {
        return CircuitBreaker.of(name, CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(window)
                .minimumNumberOfCalls(minimum)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build());
    }

    private static void sameScenarioAsHandWritten() {
        head("1. one hundred calls to a service that is down");

        Downstream svc = new Downstream();
        svc.goDown();
        svc.slowTo(20);

        CircuitBreaker cb = countBased("orders");
        Supplier<String> guarded = CircuitBreaker.decorateSupplier(cb, svc::call);

        long start = System.nanoTime();
        int refused = 0;
        for (int i = 0; i < 100; i++) {
            try { guarded.get(); }
            catch (CallNotPermittedException e) { refused++; }
            catch (RuntimeException e) { /* a real failure */ }
        }
        long ms = (System.nanoTime() - start) / 1_000_000;

        System.out.println("  config: count based window of 10, opens above a 50 percent");
        System.out.println("          failure rate, once at least 5 calls have been seen");
        System.out.println();
        System.out.printf("  calls that reached the service : %d%n", svc.received());
        System.out.printf("  refused without calling        : %d%n", refused);
        System.out.printf("  total time                     : %d ms%n", ms);
        System.out.printf("  breaker state                  : %s%n", cb.getState());
        System.out.println();
        System.out.println("  The hand written breaker in the other project gave 5 calls,");
        System.out.println("  95 refused and 103 ms. The same shape, from 20 lines of config.");
    }

    // ------------------------------------------------------------------ 2

    /**
     * How many calls reach the service before each breaker opens, over ten
     * runs of the same scenario. The interesting number turned out not to be
     * whether they open but how much the answer moves between runs.
     */
    private static void theCaseForTheLibrary() {
        head("2. a service that fails 30 percent of the time, ten runs each");
        System.out.println("  Not down, just unwell. Each run makes 300 calls.");
        System.out.println();

        int[] consecutiveTrips = new int[10];
        int[] rateTrips = new int[10];
        int[] wideTrips = new int[10];

        for (int run = 0; run < 10; run++) {
            long seed = 100 + run;

            Downstream a = new Downstream(seed);
            a.failSometimes(30);
            ConsecutiveBreaker cons = new ConsecutiveBreaker(5);
            int reachedA = -1;
            for (int i = 0; i < 300; i++) {
                if (!cons.allow()) { if (reachedA < 0) reachedA = a.received(); continue; }
                try { a.call(); cons.success(); } catch (RuntimeException e) { cons.failure(); }
            }
            consecutiveTrips[run] = cons.isOpen() ? (reachedA < 0 ? a.received() : reachedA) : -1;

            Downstream b = new Downstream(seed);
            b.failSometimes(30);
            CircuitBreaker rate = countBased("rate" + run);
            Supplier<String> guarded = CircuitBreaker.decorateSupplier(rate, b::call);
            int reachedB = -1;
            for (int i = 0; i < 300; i++) {
                try { guarded.get(); }
                catch (CallNotPermittedException e) { if (reachedB < 0) reachedB = b.received(); }
                catch (RuntimeException e) { }
            }
            rateTrips[run] = rate.getState() == CircuitBreaker.State.OPEN
                    ? (reachedB < 0 ? b.received() : reachedB) : -1;

            Downstream c = new Downstream(seed);
            c.failSometimes(30);
            CircuitBreaker wide = countBased("wide" + run, 100, 50);
            Supplier<String> guardedWide = CircuitBreaker.decorateSupplier(wide, c::call);
            int reachedC = -1;
            for (int i = 0; i < 300; i++) {
                try { guardedWide.get(); }
                catch (CallNotPermittedException e) { if (reachedC < 0) reachedC = c.received(); }
                catch (RuntimeException e) { }
            }
            wideTrips[run] = wide.getState() == CircuitBreaker.State.OPEN
                    ? (reachedC < 0 ? c.received() : reachedC) : -1;
        }

        report("hand written, 5 failures IN A ROW", consecutiveTrips);
        report("Resilience4j, 50 percent over a window of 10", rateTrips);
        report("Resilience4j, 50 percent over a window of 100", wideTrips);
    }

    private static void report(String label, int[] trips) {
        int opened = 0, min = Integer.MAX_VALUE, max = 0, total = 0;
        StringBuilder each = new StringBuilder();
        for (int t : trips) {
            each.append(t < 0 ? "never" : String.valueOf(t)).append(" ");
            if (t >= 0) { opened++; total += t; min = Math.min(min, t); max = Math.max(max, t); }
        }
        System.out.println("  " + label);
        System.out.println("    calls before it opened, per run: " + each.toString().trim());
        if (opened == 0) {
            System.out.println("    never opened in any run");
        } else {
            System.out.printf("    opened in %d of 10 runs, from %d to %d calls, average %d%n",
                    opened, min, max, total / opened);
        }
        System.out.println();
    }

    // ------------------------------------------------------------------ 3

    private static void theOrderQuestion() {
        head("3. retry outside the breaker, or inside it");
        System.out.println("  20 logical calls, 3 attempts each, against a service that is down.");
        System.out.println();

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1))
                .build();

        // Retry on the OUTSIDE: each attempt passes through the breaker
        Downstream a = new Downstream(); a.goDown();
        CircuitBreaker cbA = countBased("outside");
        Supplier<String> throughBreaker = CircuitBreaker.decorateSupplier(cbA, a::call);
        Retry retryA = Retry.of("outside", retryConfig);
        Supplier<String> outside = Retry.decorateSupplier(retryA, throughBreaker);
        for (int i = 0; i < 20; i++) { try { outside.get(); } catch (RuntimeException e) { } }

        // Retry on the INSIDE: the breaker sees one call that secretly made three
        Downstream b = new Downstream(); b.goDown();
        Retry retryB = Retry.of("inside", retryConfig);
        Supplier<String> withRetries = Retry.decorateSupplier(retryB, b::call);
        CircuitBreaker cbB = countBased("inside");
        Supplier<String> inside = CircuitBreaker.decorateSupplier(cbB, withRetries);
        for (int i = 0; i < 20; i++) { try { inside.get(); } catch (RuntimeException e) { } }

        System.out.printf("  %-40s %12s %10s%n", "", "calls that", "breaker");
        System.out.printf("  %-40s %12s %10s%n", "", "reached it", "state");
        System.out.println("  " + "-".repeat(64));
        System.out.printf("  %-40s %12d %10s%n",
                "Retry.decorate(CircuitBreaker.decorate(f))", a.received(), cbA.getState());
        System.out.printf("  %-40s %12d %10s%n",
                "CircuitBreaker.decorate(Retry.decorate(f))", b.received(), cbB.getState());
        System.out.println();
        System.out.println("  Resilience4j documents the order as Retry outermost, then");
        System.out.println("  CircuitBreaker, then RateLimiter, then TimeLimiter, then Bulkhead.");
        System.out.println("  The numbers above are why: get it backwards and the breaker only");
        System.out.println("  sees a third of what is really happening.");
    }

    // ------------------------------------------------------------------ 4

    private static void bulkheadAndRateLimiter() throws Exception {
        head("4. bulkhead and rate limiter");

        Bulkhead bulkhead = Bulkhead.of("reports", BulkheadConfig.custom()
                .maxConcurrentCalls(4)
                .maxWaitDuration(Duration.ZERO)
                .build());

        Downstream slow = new Downstream();
        slow.slowTo(500);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger ran = new AtomicInteger(), rejected = new AtomicInteger();

        for (int i = 0; i < 20; i++) {
            futures.add(pool.submit(() -> {
                try {
                    Callable<String> c = Bulkhead.decorateCallable(bulkhead, slow::call);
                    c.call();
                    ran.incrementAndGet();
                } catch (Exception e) {
                    rejected.incrementAndGet();
                }
            }));
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        System.out.println("  bulkhead of 4, hit with 20 calls at once:");
        System.out.printf("    ran %d, rejected immediately %d, service received %d%n",
                ran.get(), rejected.get(), slow.received());

        RateLimiter limiter = RateLimiter.of("prices", RateLimiterConfig.custom()
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build());

        Downstream fast = new Downstream();
        Supplier<String> limited = RateLimiter.decorateSupplier(limiter, fast::call);
        int allowed = 0, blocked = 0;
        for (int i = 0; i < 25; i++) {
            try { limited.get(); allowed++; } catch (RuntimeException e) { blocked++; }
        }

        System.out.println();
        System.out.println("  rate limiter of 10 per second, hit with 25 calls at once:");
        System.out.printf("    allowed %d, blocked %d, service received %d%n",
                allowed, blocked, fast.received());
        System.out.println();
        System.out.println("  A bulkhead limits how many run AT ONCE. A rate limiter limits");
        System.out.println("  how many run PER PERIOD. They are different questions and a");
        System.out.println("  service under strain often needs both.");
    }
}
