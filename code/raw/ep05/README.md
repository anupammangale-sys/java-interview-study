# Episode 5 demos

Needs Java 11+ for single-file source launch. Captured on Java 24.

## 1. The counter, held six ways

```bash
java CounterRace.java
```

8 threads, 500,000 increments each, so a correct counter ends at 4,000,000.

| How the counter is held | Best | Worst | Best ms | Correct |
|---|---|---|---|---|
| `long` | 4,000,000 | 1,124,713 | 0 | no |
| `volatile long` | 1,278,827 | 1,215,619 | 56 | no |
| `synchronized` | 4,000,000 | 4,000,000 | 185 | yes |
| `ReentrantLock` | 4,000,000 | 4,000,000 | 96 | yes |
| `AtomicLong` | 4,000,000 | 4,000,000 | 57 | yes |
| `LongAdder` | 4,000,000 | 4,000,000 | 5 | yes |

`volatile` lost about 70 percent of the updates, was never once correct, and was
slower than `AtomicLong` which was always correct.

The plain counter finishing in 0 ms is itself the lesson: with a non-volatile
field the compiler may keep the value in a register and collapse the loop, which
is why its results swing between 1.1 million and the full 4 million.

## 2. Waiting for a task on the same pool

```bash
java PoolDeadlock.java
```

| Setup | Result |
|---|---|
| 2 threads, 2 tasks, same pool | stuck |
| 4 threads, 2 tasks, same pool | finished |
| 4 threads, 4 tasks, same pool | stuck |
| 8 threads, 8 tasks, same pool | stuck |
| 2 threads, 8 tasks, separate inner pool | finished |

Row two is the trap: doubling the pool looks like the fix until row three.

**This demo needed a start barrier to be trustworthy.** Without it, whether the
deadlock appeared depended on a race in lazy thread creation, and "8 threads, 8
tasks" would sometimes finish. The barrier makes every outer task start before
any asks for an inner thread, and the result is now identical across runs. It
waits for `min(taskCount, poolSize)`, not `taskCount`, because with fewer threads
than tasks the latter can never be reached.

## 3. What a pool throws away

```bash
java ExecutorBehaviour.java
```

2 threads, queue of 4, 20 tasks submitted at once.

| Policy | Ran | Rejected | On caller |
|---|---|---|---|
| AbortPolicy (default) | 6 | 14 | 0 |
| CallerRunsPolicy | 20 | 0 | 5 |
| DiscardPolicy | 6 | 0 | 0 |
| DiscardOldestPolicy | 6 | 0 | 0 |

DiscardPolicy is the dangerous one: zero rejected, zero errors, 14 tasks gone.

## 4. Four locks, and the result that flips

```bash
java LockBenchmark.java
```

7 readers, 1 writer. Rough measurement, warmup then best of four.

Tiny critical section (reads/sec): synchronized 19.3M, ReentrantLock 38.5M,
**ReadWriteLock 10.3M**, StampedLock optimistic 928.7M.

Longer critical section (reads/sec): synchronized 1.6M, ReentrantLock 1.8M,
**ReadWriteLock 3.9M**, StampedLock optimistic 7.8M.

The read write lock goes from worst to best purely by holding the lock longer.
The first version of this benchmark only had the tiny case, which would have
published "ReadWriteLock is slower than a plain lock" as if it were a general
truth. It is not: it is a statement about critical section length.
