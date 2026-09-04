/**
 * A circuit breaker, written out in full so the state machine is visible.
 * This is the whole thing: three states and two counters.
 *
 *   CLOSED     calls go through. Enough failures in a row and it opens.
 *   OPEN       calls are refused immediately, without touching the service.
 *              After a cooling off period it moves to half open.
 *   HALF_OPEN  a few trial calls are allowed through. If they work it closes.
 *              If one fails it opens again and the clock restarts.
 */
public class Breaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final String name;
    private final int failureThreshold;   // failures in a row before opening
    private final long openMillis;        // how long to stay open
    private final int trialCalls;         // how many calls to test with

    private State state = State.CLOSED;
    private int consecutiveFailures;
    private long openedAt;
    private int trialsStarted;
    private int trialsSucceeded;

    private int allowed;
    private int refused;
    private final StringBuilder transitions = new StringBuilder();

    public Breaker(String name, int failureThreshold, long openMillis, int trialCalls) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.openMillis = openMillis;
        this.trialCalls = trialCalls;
    }

    /** Ask before calling. False means refuse without touching the service. */
    public synchronized boolean allow() {
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - openedAt >= openMillis) {
                moveTo(State.HALF_OPEN);
                trialsStarted = 0;
                trialsSucceeded = 0;
            } else {
                refused++;
                return false;
            }
        }
        if (state == State.HALF_OPEN) {
            if (trialsStarted >= trialCalls) { refused++; return false; }
            trialsStarted++;
        }
        allowed++;
        return true;
    }

    public synchronized void recordSuccess() {
        if (state == State.HALF_OPEN) {
            if (++trialsSucceeded >= trialCalls) {
                moveTo(State.CLOSED);
                consecutiveFailures = 0;
            }
        } else {
            consecutiveFailures = 0;
        }
    }

    public synchronized void recordFailure() {
        if (state == State.HALF_OPEN) {
            trip();
            return;
        }
        if (++consecutiveFailures >= failureThreshold) {
            trip();
        }
    }

    private void trip() {
        moveTo(State.OPEN);
        openedAt = System.currentTimeMillis();
        consecutiveFailures = 0;
    }

    private void moveTo(State next) {
        if (next != state) {
            transitions.append(state).append(" -> ").append(next).append("  ");
            state = next;
        }
    }

    public synchronized State state()   { return state; }
    public synchronized int allowed()   { return allowed; }
    public synchronized int refused()   { return refused; }
    public synchronized String path()   { return transitions.toString().trim(); }
    public String name()                { return name; }
}
