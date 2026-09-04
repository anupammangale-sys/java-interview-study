# Episode 7 demos

Captured on Java 24.0.2, 12 processors.

## 1. What a stream actually does

```bash
java StreamLaziness.java
```

**Nothing runs without a terminal operation.** A pipeline with counters in every
stage touched `0` elements when built, and `6` after adding `toList()`.

**Elements go through one at a time**, not stage by stage:

```
filter ann
  map ann
filter bob
  map bob
filter cat
  map cat
```

**Short-circuiting**, on a stream of 1,000,000:

```
findFirst() gave 14
filter ran 7 times, map ran 1 time, out of 1,000,000
count() gave 142,857 and had to look at 1,000,000 elements
```

**A stream is single use.** Reusing one throws
`IllegalStateException: stream has already been operated upon or closed`.

## 2. Parallel streams

```bash
java ParallelStreams.java
```

| Work | Sequential | Parallel | Verdict |
|---|---|---|---|
| 1,000 items, add up | 24 us | 96 us | **4.0x SLOWER** |
| 2,000,000 items, add up | 3,905 us | 1,064 us | 3.7x faster |
| 20,000 items, real work each | 26,277 us | 3,106 us | 8.5x faster |
| 20,000 LinkedList, real work | 27,080 us | 3,478 us | 7.8x faster |
| 1,000,000 ArrayList, add up | 2,758 us | 670 us | 4.1x faster |
| 1,000,000 LinkedList, add up | 2,884 us | 3,186 us | **no gain** |

Boxing, both sequential: 2,000,000 boxed Integers 5,394 us against 2,000,000
primitive ints 520 us, **10.4x faster**. That beat going parallel in every row
above, costs nothing, and keeps the code sequential.

The first version of this benchmark only tested LinkedList with expensive work,
where it parallelised fine at 7.8x, and would have concluded the source
structure does not matter much. Adding the cheap-work case showed the opposite:
identical size, ArrayList gained 4.1x and LinkedList gained nothing. The source
matters exactly when the work is cheap enough for splitting cost to dominate.

## 3. reduce against collect, and the collectors

```bash
java ReduceVsCollect.java
```

Joining strings:

| Items | reduce | collect | Ratio |
|---|---|---|---|
| 1,000 | 0 ms | 0 ms | 4x |
| 5,000 | 11 ms | 0 ms | 58x |
| 20,000 | 115 ms | 0 ms | 322x |
| 50,000 | 626 ms | 1 ms | **510x** |

The reduce column grows with the square of the item count, because Strings
cannot be changed and every step copies everything accumulated so far.

Also prints real output for `groupingBy`, `mapping`, `counting`,
`partitioningBy`, `toMap` with a merge function, `joining` and `teeing`,
including the detail that **partitioningBy always returns both keys** even when
one side is empty, where `groupingBy` would have no entry at all.
