# Episode 4 demos

Needs Java 11+ for single-file source launch. Captured on Java 24.

## 1. Look inside a real HashMap

```bash
java --add-opens java.base/java.util=ALL-UNNAMED HashMapInternals.java
```

The `--add-opens` is required because modern Java hides these internals.
Shows capacity doubling at load factor 0.75, how unevenly 1000 keys actually
spread, and the treeify rule. The third section is the interesting one: the
chain reached 10 entries and the map resized twice before finally building a
tree, because treeify needs 8 entries in a bucket AND a capacity of at least 64.

## 2. ArrayList vs LinkedList

```bash
java ListBenchmark.java
```

Rough timing, not a rigorous benchmark: warmup rounds then best of seven. Fine
for differences of ten times or more, which is what shows up. Measured:

| Operation | ArrayList | LinkedList |
|---|---|---|
| get by random index | 91 us | 628,300 us |
| add at the end | 193 us | 70 us |
| add at the front | 143,017 us | 76 us |
| insert in the middle | 71,721 us | 1,404,909 us |
| walk the whole list | 68 us | 349 us |
| remove while iterating | 164,400 us | 404 us |

LinkedList is 1877x faster inserting at the front and 20x SLOWER inserting in
the middle. The insert is cheap either way; getting to the position is not.

## 3. Eight threads writing to one map

```bash
java ConcurrentMapDemo.java
```

Results vary per run, which is the point. One capture:

```
HashMap        attempt 1: DID NOT FINISH - threads still spinning after 10s
               attempt 2: 105,688 entries  LOST 54,312
               attempt 3: DID NOT FINISH - threads still spinning after 10s
               attempt 4: 160,000 entries  correct
               attempt 5: 151,665 entries  LOST 8,335
```

Attempt 4 is the dangerous one. It sometimes works, which is why this bug
reaches production. synchronizedMap and ConcurrentHashMap were correct 5/5.

The demo uses daemon threads and a 10 second watchdog, so a spinning HashMap
cannot hang your terminal.

## 4. Mutable keys and changing while walking

```bash
java KeysAndIteration.java
```

Shows an entry becoming permanently unreachable after its key changes: get
returns null, remove does nothing, but size stays 1 and it is still visible when
iterating. Also shows that fail-fast is NOT guaranteed: removing from a 5
element list throws, but the same removal on a 3 element list throws nothing and
silently skips the last element.
