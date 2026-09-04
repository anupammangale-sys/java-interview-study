/**
 * The circuit breaker, measured. Everything here is the hand written Breaker,
 * so nothing is hidden inside a library.
 *
 *   java Main.java
 */
public class Main {

    static void head(String s) {
        System.out.println();
        System.out.println("=== " + s + " ===");
    }

    public static void main(String[] args) throws Exception {
        noProtectionVersusBreaker();
        theOrderQuestion();
        recovery();
    }

    // ------------------------------------------------------------------ 1

    private static void noProtectionVersusBreaker() {
        head("1. one hundred calls to a service that is down");

        FlakyService plain = new FlakyService();
        plain.goDown();
        plain.slowTo(20);                 // a failing call is rarely instant
        long start = System.nanoTime();
        int failures = 0;
        for (int i = 0; i < 100; i++) {
            try { plain.call("x"); } catch (RuntimeException e) { failures++; }
        }
        long plainMs = (System.nanoTime() - start) / 1_000_000;

        FlakyService guarded = new FlakyService();
        guarded.goDown();
        guarded.slowTo(20);
        Breaker breaker = new Breaker("orders", 5, 10_000, 2);
        start = System.nanoTime();
        int refusedFast = 0;
        for (int i = 0; i < 100; i++) {
            if (!breaker.allow()) { refusedFast++; continue; }
            try {
                guarded.call("x");
                breaker.recordSuccess();
            } catch (RuntimeException e) {
                breaker.recordFailure();
            }
        }
        long guardedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("  %-22s %10s %14s %12s%n",
                "", "calls that", "refused", "total");
        System.out.printf("  %-22s %10s %14s %12s%n",
                "", "reached it", "without calling", "time");
        System.out.println("  " + "-".repeat(62));
        System.out.printf("  %-22s %10d %14d %10d ms%n",
                "no protection", plain.callsReceived(), 0, plainMs);
        System.out.printf("  %-22s %10d %14d %10d ms%n",
                "circuit breaker", guarded.callsReceived(), refusedFast, guardedMs);
        System.out.println();
        System.out.println("  breaker state: " + breaker.state() + ", path: " + breaker.path());
        System.out.printf("  The failing service took %d calls instead of %d, and the caller%n",
                guarded.callsReceived(), plain.callsReceived());
        System.out.printf("  got its answer in %d ms instead of %d.%n", guardedMs, plainMs);
    }

    // ------------------------------------------------------------------ 2

    /** Retry on the OUTSIDE: every attempt is checked by the breaker. */
    private static void retryOutsideBreaker(FlakyService svc, Breaker b, int attempts) {
        for (int a = 0; a < attempts; a++) {
            if (!b.allow()) return;                   // breaker open, stop trying
            try { svc.call("x"); b.recordSuccess(); return; }
            catch (RuntimeException e) { b.recordFailure(); }
        }
    }

    /** Retry on the INSIDE: the breaker sees one call, which secretly made three. */
    private static void retryInsideBreaker(FlakyService svc, Breaker b, int attempts) {
        if (!b.allow()) return;
        for (int a = 0; a < attempts; a++) {
            try { svc.call("x"); b.recordSuccess(); return; }
            catch (RuntimeException e) { /* keep trying inside */ }
        }
        b.recordFailure();                            // one failure for three calls
    }

    private static void theOrderQuestion() {
        head("2. retry outside the breaker, or inside it");
        System.out.println("  20 logical calls, 3 attempts each, against a service that is down.");
        System.out.println("  Both breakers open after 5 failures.");
        System.out.println();

        FlakyService a = new FlakyService(); a.goDown();
        Breaker outside = new Breaker("outside", 5, 10_000, 2);
        for (int i = 0; i < 20; i++) retryOutsideBreaker(a, outside, 3);

        FlakyService b = new FlakyService(); b.goDown();
        Breaker inside = new Breaker("inside", 5, 10_000, 2);
        for (int i = 0; i < 20; i++) retryInsideBreaker(b, inside, 3);

        System.out.printf("  %-38s %12s %10s%n", "", "calls that", "breaker");
        System.out.printf("  %-38s %12s %10s%n", "", "reached it", "state");
        System.out.println("  " + "-".repeat(62));
        System.out.printf("  %-38s %12d %10s%n",
                "retry OUTSIDE, breaker sees attempts", a.callsReceived(), outside.state());
        System.out.printf("  %-38s %12d %10s%n",
                "retry INSIDE, breaker sees one call", b.callsReceived(), inside.state());
        System.out.println();
        System.out.printf("  The service that is already down took %d calls instead of %d,%n",
                b.callsReceived(), a.callsReceived());
        System.out.println("  which is the difference between helping it and finishing it off.");
    }

    // ------------------------------------------------------------------ 3

    private static void recovery() {
        head("3. opening, waiting, and closing again");

        FlakyService svc = new FlakyService();
        Breaker breaker = new Breaker("recovery", 3, 300, 2);
        StringBuilder line = new StringBuilder();

        svc.goDown();
        for (int i = 0; i < 6; i++) line.append(oneCall(svc, breaker));
        line.append(" |");

        FlakyService.sleep(350);                       // wait out the open period
        svc.comeBack();
        for (int i = 0; i < 6; i++) line.append(oneCall(svc, breaker));

        System.out.println("  each character is one call, in order:");
        System.out.println("    " + line);
        System.out.println();
        System.out.println("    .  went through and worked");
        System.out.println("    x  went through and failed");
        System.out.println("    -  refused by the breaker without touching the service");
        System.out.println("    |  waited out the open period, and the service recovered");
        System.out.println();
        System.out.println("  state path: " + breaker.path());
        System.out.println("  final state: " + breaker.state());
        System.out.println("  calls the service actually received: " + svc.callsReceived()
                + " out of 12 attempted");
    }

    private static String oneCall(FlakyService svc, Breaker b) {
        if (!b.allow()) return "-";
        try { svc.call("x"); b.recordSuccess(); return "."; }
        catch (RuntimeException e) { b.recordFailure(); return "x"; }
    }
}
