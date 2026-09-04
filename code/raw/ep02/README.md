# Episode 2 demos

Needs Java 11+. The JDK tools live in your JDK bin folder. If `jstack` is not on
your PATH, use the full path, for example:
`"C:\Program Files\Java\jdk-24\bin\jstack.exe" <pid>`

Each program prints its own process id when it starts.

## 1. Deadlock
```bash
java DeadlockDemo.java
jstack <pid> | grep -A 20 "Found one Java-level deadlock"
```
Two threads take two locks in opposite order. Java detects the circle itself and
names both threads. Captured output is in `deadlock_dump.txt`.

## 2. One thread burning CPU
```bash
java HotCpuDemo.java
jstack <pid>
```
Compare `cpu=` against `elapsed=` in each entry. The busy thread showed
`cpu=5500.00ms elapsed=5.76s`, about 95 percent of one core. The idle threads
showed `cpu=0.00ms`. Captured output is in `cpu_dump.txt`.

## 3. Thread pool starvation
```bash
java PoolStarvationDemo.java
jstack <pid> | grep -A 3 "pool-1-thread"
```
Three threads, ten jobs, one slow call with no timeout. All three threads end up
in the same place and seven jobs wait forever. Memory and CPU both look fine.
Captured output is in `pool_dump.txt`.
