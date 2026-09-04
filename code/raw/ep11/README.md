# Episode 11 demos

Captured on Java 24.0.2. One notification service, built the way the low level
design round asks for it, plus the two measurements that decide whether the
design is any good.

## 1. The service

```bash
cd service && java Main.java
```

Fifteen small files. Three channels behind one interface, a factory, two
listeners, a bounded queue with three workers, an exception hierarchy, retries,
and a dead letter queue.

The whole retry decision is one line, because the exception knows the answer:

```java
if (!e.worthRetrying()) { giveUp(...); }
```

What that buys, from the real run:

```
bad address, cannot ever work:
  N-4 GAVE UP: permanent: not an email address: asha-at-example.com

provider busy, worth trying again:
  retry N-5 in 50 ms, attempt 1 failed: sms provider busy
  retry N-5 in 100 ms, attempt 2 failed: sms provider busy
  [sms] delivered to +919000000001 on attempt 3
```

**The permanent failure used 1 attempt. The temporary one used 3.** Nothing in
the service asked what kind of failure it was; it asked the exception.

The same request submitted twice with the same idempotency key was sent once:

```
[email] delivered to ravi@example.com
ignored duplicate of N-7, key pay-9912
```

### The queue is bounded, and that number is not stable

Queue capacity 10, three workers, 40 notifications submitted at once. Four runs:

| run | accepted | rejected |
|---|---|---|
| 1 | 31 | 9 |
| 2 | 27 | 13 |
| 3 | 29 | 11 |
| 4 | 26 | 14 |

**This varies on purpose and it is worth saying so out loud in an interview.**
How many get rejected depends on how fast three workers drain the queue against
how fast the caller fills it. You cannot reason your way to the number, you
measure it, and then you pick the capacity from what you measured.

Stable across every run: 4 retries, 1 dead lettered, and the dead letter queue
holding exactly the permanently broken one after a single attempt.

## 2. Rate limiters

```bash
java RateLimiterCompare.java
```

Limit: 10 per second. The clock is a plain number the test moves forward, so
the results are about the algorithm and not about thread scheduling.

**A burst straddling a window boundary.** Ten requests just before the 1000 ms
mark and ten just after, so twenty inside 60 ms:

```
limiter                 allowed   worst 1s   verdict
fixed window                 20         20   over 10 by 10
sliding window log           10         10   never over 10
token bucket 20/10           20         20   over 10 by 10
```

**The fixed window and the token bucket allowed exactly the same 20 requests.**
That is the interesting part, because the reasons are opposite. Slide the same
burst across every millisecond of the window and the difference shows up:

```
limiter                   best case     worst case
fixed window                     10             20   depends on timing
sliding window log               10             10   same every time
token bucket 20/10               20             20   same every time
```

The token bucket lets 20 through **every time, because 20 is the capacity you
set**. The fixed window lets 20 through **only when the traffic happens to land
on a boundary**. One is a dial. The other is luck.

A quiet client that then sends 30 at once: fixed window 10, sliding window 10,
token bucket 20, which is the burst it was configured to forgive.

## 3. Retries and the dead letter queue

```bash
java RetryAndDlq.java
```

**Everyone fails at the same moment.** 500 clients, three retries each, backoff
100 ms doubling:

| | retries | 10 ms slices used | worst slice |
|---|---|---|---|
| no jitter, plain backoff | 1500 | **3** | **500** |
| with jitter | 1500 | 66 | 78 |

Plain exponential backoff does not spread anything out. It moves the whole crowd
together, so the server that just failed is hit by all 500 clients at once,
three times over. Jitter cut the worst slice from 500 to 78, about **6.4 times
lower**.

**What it costs to retry something that can never work.** 200 notifications, 60
of them permanently broken, up to 4 attempts:

| | attempts used | time spent waiting |
|---|---|---|
| retry everything the same way | 520 | 56,000 ms |
| ask the exception first | 340 | 14,000 ms |

**180 wasted attempts and 42 seconds of waiting**, removed by one method on the
exception base class.
