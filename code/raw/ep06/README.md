# Episode 6 demos

Captured on Java 24.0.2, on a machine with 12 processors.

## 1. How many threads can exist

```bash
java -Xmx1g ThreadScale.java platform
java -Xmx1g ThreadScale.java virtual
```

**Run each kind in its own JVM.** The first version of this ran both in one
program and the numbers were nonsense: tearing down 50,000 platform threads was
still happening while the virtual measurement started, which reported 1,000
virtual threads taking 25.8 seconds.

| Asked for | Platform | Virtual |
|---|---|---|
| 1,000 | 37 ms | 9 ms |
| 10,000 | 513 ms | 18 ms |
| 20,000 | 2,129 ms | - |
| 50,000 | 12,605 ms | - |
| 100,000 | - | 76 ms |
| 500,000 | - | 398 ms |
| 1,000,000 | - | 656 ms |

Platform cost grows faster than the count. Virtual threads created a million in
less time than platform threads took for twenty thousand.

## 2. Blocking throughput

```bash
java BlockingThroughput.java
```

10,000 tasks, each waiting 100 ms.

| Executor | Wall time | tasks/threads x wait |
|---|---|---|
| fixed pool, 200 threads | 5,043 ms | about 5,000 ms |
| fixed pool, 1000 threads | 1,048 ms | about 1,000 ms |
| fixed pool, 2000 threads | 572 ms | about 500 ms |
| one virtual thread per task | 203 ms | about 100 ms |

The arithmetic matches the measurement in every row.

## 3. Does synchronized still pin? (the interesting one)

```bash
java PinningTest.java
```

2,000 virtual threads, each locking **its own** object then sleeping 200 ms.
No contention at all, so the only thing that could serialise them is pinning.

```
Carrier threads available: 12
if pinning happens, expect roughly 33,200 ms
if it does not, expect roughly 200 ms

synchronized (own monitor)               236 ms
ReentrantLock (own lock)                 212 ms
```

**On Java 24, synchronized does not pin.** JEP 491 removed it. The advice to
replace synchronized with ReentrantLock before adopting virtual threads was
correct for Java 21 to 23 and is out of date on 24. Run this on an older JDK and
the first row should be around 33 seconds instead.

What still pins is code blocking below Java in native C, because the stack
cannot be moved to the heap. Not demonstrated here, since it needs a native
library to show.

## 4. Structured concurrency (preview)

```bash
java --enable-preview --source 24 StructuredDemo.java
```

Two subtasks, 100 ms and 300 ms, run in parallel. When one fails the other is
cancelled and the whole scope fails:

```
1. both succeed:
   Order[user=user-42, cart=cart-7]

2. one fails, so the other is cancelled:
   whole scope failed: cart service is down
```

Still a preview feature and the API has changed between releases, so the flag is
required and this should not go into production yet.
