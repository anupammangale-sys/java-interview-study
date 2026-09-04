# Demo — run it yourself

Needs Java 11+ (single-file source launch). Verified on Java 24.

## The leak — dies in ~30 seconds

```bash
mkdir -p dumps
java -Xmx128m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./dumps LeakDemo.java
```

Heap climbs 38 MB -> 123 MB, then `OutOfMemoryError: Java heap space`.
A ~131 MB `.hprof` lands in `dumps/`. Open it in Eclipse MAT.

## The fix — same load, bounded lifetime, never dies

```bash
java -Xmx128m FixedDemo.java
```

Watch `entries held` pin at 1000 while `cached` keeps climbing.
The heap still moves a little — that is uncollected garbage, not retention.

## Take a dump from a live process instead

```bash
jcmd <pid> GC.heap_dump /full/path/app.hprof
```

Fails if the file already exists. And never do this on a node taking live
traffic — the pause scales with heap size. Pull it from the load balancer first.

## The ThreadLocal leak — 403 MB vs 3 MB

```bash
java -Xmx512m ThreadLocalLeakDemo.java          # without remove()
java -Xmx512m ThreadLocalLeakDemo.java fixed    # with remove() in finally
```

200 threads, 1 MB each, all tasks finished, GC forced. Without `remove()` the
payloads are still held because the threads went back to the pool instead of dying.
