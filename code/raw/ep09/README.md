# Episode 9 demos

Captured on Java 24.0.2. The "broken" results are real, not claimed.

## 1. Breaking a Singleton

```bash
java SingletonBreaking.java
```

Three attacks against a textbook Singleton, all successful:

```
reflection    -> same instance? false   hash 45ca843 vs 11c9af63
serialization -> same instance? false   hash 45ca843 vs 2df9b86
cloning       -> same instance? false   hash 45ca843 vs 37654521
```

The defended version:

```
reflection    -> blocked: IllegalStateException: already created, use getInstance()
serialization -> same instance? true    (readResolve did this)
```

The enum:

```
reflection    -> blocked: IllegalArgumentException: Cannot reflectively create enum objects
serialization -> same instance? true    (the language guarantees this)
cloning       -> not possible, enums cannot be cloned
```

The enum's defences come from the runtime rather than from code you have to
remember to write, which is the whole argument for it.

## 2. The lazy initialisation race

```bash
java LazyInitRace.java
```

32 threads calling `getInstance()` at the same moment, five attempts:

| How it is written | Distinct instances | Correct |
|---|---|---|
| no protection | 29, 30, 29, 29, 31 | no |
| synchronized method | 1, 1, 1, 1, 1 | yes |
| double checked + volatile | 1, 1, 1, 1, 1 | yes |
| holder idiom | 1, 1, 1, 1, 1 | yes |

**What this does not show.** It demonstrates the race anyone can reproduce:
several threads passing the null check together. It does NOT demonstrate the
subtler reordering problem that `volatile` exists to fix, where a thread sees a
reference to a half constructed object. That needs weaker memory ordering than
an ordinary desktop processor, so the demo says so rather than pretending.

## 3. The other patterns

```bash
java PatternsInAction.java
```

Strategy as an if-else chain, a map of functions, and an enum, all producing
identical results. Factory. Builder, including `build()` throwing
`needs a topping`. Observer, including unsubscribing so the second event
reaches only one listener. Decorator, wrapping `'  hello  '` through trim,
uppercase and brackets to `'[HELLO]'`. Adapter, turning
`Notifier.notify(who, text)` into `LegacySms.dispatch("+44123|your order shipped")`.
