# Episode 3 demos

Needs Java 11+ for single-file source launch. Captured on Java 24.

## Compare the collectors

```bash
java -Xmx512m -XX:+UseSerialGC   -Xlog:gc GcDemo.java
java -Xmx512m -XX:+UseParallelGC -Xlog:gc GcDemo.java
java -Xmx512m -XX:+UseG1GC       -Xlog:gc GcDemo.java
java -Xmx512m -XX:+UseZGC        -Xlog:gc,gc+phases GcDemo.java
```

ZGC needs the extra `gc+phases` logging, because at plain `gc` level it prints
cycle durations rather than actual freeze times. Comparing a ZGC cycle against a
G1 pause is not a like-for-like comparison and makes ZGC look far worse than it is.

## What was measured (512 MB heap, same workload)

| Collector | Stops | Full | Total freeze | Worst freeze | Wall time |
|---|---|---|---|---|---|
| Serial   | 50  | 2 | 214.4 ms | 22.62 ms | 487 ms |
| Parallel | 28  | 1 | 124.9 ms | 23.45 ms | 386 ms |
| G1       | 34  | 0 |  97.6 ms | 15.54 ms | 378 ms |
| ZGC      | 109 | 0 |   0.64 ms |  0.020 ms | 438 ms |

## Under pressure

At `-Xmx256m` with the same workload, Serial and Parallel both hit full
collections and ZGC stalled and aborted outright, because it had no room to work
in. That is worth knowing: shorter pauses cost memory as well as throughput.

## Container heap sizing

```bash
java -XX:MaxRAMPercentage=70 -XX:+PrintFlagsFinal -version | grep MaxHeapSize
```

On a 16 GB machine this printed 4054 MB at 25 percent (the default), 8108 MB at
50 percent and 11352 MB at 70 percent.
