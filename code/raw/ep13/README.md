# Episode 13 demos

Two projects. `handbuilt/` is plain Java with no dependencies, so the state
machine is visible. `resilience4j/` runs the same scenarios through the real
library, Resilience4j 2.2.0, on Java 24.

```bash
cd handbuilt     && java Main.java && java Isolation.java
cd resilience4j  && mvn compile && java -cp "target/classes;$(cat cp.txt)" demo.Main
```

Every number below came from those two commands.

## 1. One hundred calls to a service that is down

| | calls that reached it | refused without calling | total time |
|---|---|---|---|
| no protection | 100 | 0 | 2066 ms |
| hand written breaker | **5** | 95 | **103 ms** |
| Resilience4j | **5** | 95 | **125 ms** |

The failing service took 5 calls instead of 100, and the caller got its answer
twenty times faster. Both versions agree, which is the point of writing it twice:
the library is not doing anything mysterious.

## 2. The one that changed what this episode says

A service that is not down, just unwell: it fails **30 percent** of the time.
Three breakers, ten runs each, 300 calls per run. The number recorded is how
many calls reached the service before the breaker opened.

```
hand written, 5 failures IN A ROW
  per run: 236 188 never 234 never 204 7 never 51 never
  opened in 6 of 10 runs, from 7 to 236 calls, average 153

Resilience4j, 50 percent over a window of 10
  per run: 48 15 13 6 24 26 5 33 50 95
  opened in 10 of 10 runs, from 5 to 95 calls, average 31

Resilience4j, 50 percent over a window of 100
  per run: never never never never never never never never never never
  never opened in any run
```

Three things worth taking from that, and the second one was a surprise.

**Counting failures in a row is unreliable.** The hand written breaker missed the
problem entirely in 4 of 10 identical runs, and when it did notice, it took
anywhere from 7 to 236 calls. A single success resets the count, so a service
failing 30 percent of the time can keep it closed almost indefinitely.

**The window size decided the outcome, not the threshold.** Both Resilience4j
rows use the same 50 percent threshold against the same 30 percent failure rate.
One opened on every single run. The other never opened at all. With only 10
samples of a 30 percent process, exceeding 50 percent is common, so the narrow
window is largely measuring noise. The wide window measures the real rate, which
is below the threshold, so it correctly leaves the breaker closed.

**Neither is simply right.** A narrow window reacts fast and cries wolf. A wide
window is accurate and slow to protect you. That trade is the actual decision,
and it is not visible from reading the configuration.

An earlier version of this demo claimed a consecutive breaker "barely notices" a
partly failing service. The measurement disagreed: it opened, at 14 calls. The
claim was replaced with what the runs actually show.

## 3. Retry outside the breaker, or inside it

20 logical calls, 3 attempts each, against a service that is down.

| | calls that reached it |
|---|---|
| retry OUTSIDE, breaker sees every attempt | **5** |
| retry INSIDE, breaker sees one call per three | **15** |

Identical in both projects. Put the retry inside, and the breaker counts one
failure for every three real calls, so it takes three times as long to open and
the service that is already down absorbs three times the load.

This is why Resilience4j documents an order: **Retry, then CircuitBreaker, then
RateLimiter, then TimeLimiter, then Bulkhead**, outermost first.

## 4. Opening, waiting, closing again

From the hand written breaker, one character per call:

```
xxx--- |......

.  went through and worked
x  went through and failed
-  refused by the breaker without touching the service
|  waited out the open period, and the service recovered

state path: CLOSED -> OPEN   OPEN -> HALF_OPEN   HALF_OPEN -> CLOSED
calls the service actually received: 9 out of 12 attempted
```

Three failures open it. The next three are refused without a call. After the
cooling off period it allows trial calls, they succeed, and it closes.

## 5. One sick dependency, one shared thread pool

A pool of 10 threads serves two dependencies. Reports has gone slow at 2 seconds
a call; prices is healthy at 10 ms. 20 report requests arrive, then 20 price
requests. The measurement is the slowest price call, **timed from submission**,
because the whole cost of a starved pool is the wait before the task starts.

| | report calls refused | price calls that worked | slowest price call |
|---|---|---|---|
| no bulkhead | 0 | 20 | **3986 ms** |
| bulkhead, 4 permits | 16 | 20 | **60 ms** |

A call to a perfectly healthy service went from four seconds to 60 milliseconds.
The bulkhead achieves that by refusing 16 report calls, which is the trade: you
give up some of the sick dependency to keep everything else moving.

The first version of this demo timed the price calls from inside the task and
showed 25 ms against 15 ms, which proved nothing. The queue wait was the entire
effect and it was being measured out of the result.

## 6. A call that never comes back

```
no timeout      waited 3007 ms, got "eventually"
200 ms timeout  waited 213 ms, got "gave up"
```

The timeout freed the **caller** after 213 ms. Whether the work stopped is a
separate question: cancelling only interrupts, and code that ignores
interruption keeps running. A timeout with no way to stop the work still leaks
threads.

## 7. Bulkhead against rate limiter

From Resilience4j:

```
bulkhead of 4, hit with 20 calls at once:
  ran 4, rejected immediately 16, service received 4

rate limiter of 10 per second, hit with 25 calls at once:
  allowed 10, blocked 15, service received 10
```

A bulkhead limits how many run **at once**. A rate limiter limits how many run
**per period**. Different questions, and a service under strain often needs both.

## What the two projects cost

The hand written circuit breaker is about 90 lines including comments, and it
gave the same answers as the library for a service that is fully down. It fell
apart on the case in section 2, which is a partly failing service, and that is
the honest argument for the library: not that it is faster or shorter, but that
somebody has already thought about sliding windows, half open trial calls,
slow call rates and the metrics you need to tune any of it.
