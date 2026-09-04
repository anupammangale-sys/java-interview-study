# Episode 8 demos

Every solution on the episode page came from these files. They were compiled and
run, and the output shown on the page is what they actually printed.

```bash
java EmployeeQueries.java
java FrequencyAndDuplicates.java
java SortingEdges.java
java FlattenAndExceptions.java
```

## The two answers that run cleanly and give the wrong result

**1. First character appearing exactly once** (`FrequencyAndDuplicates`, item 6).

The answer everyone writes groups by character with `counting()`, filters for 1,
and takes `findFirst`. On `"swiss cheese"`:

```
with LinkedHashMap::new  ->  w      (correct)
with a plain HashMap     ->  c      (wrong)
```

`groupingBy` builds a HashMap, so "first" becomes first in hash order rather
than first in the string. The wrong version compiles, runs, and returns a
plausible letter.

**2. Sorting a map by value** (`SortingEdges`, item 6).

```
collect into LinkedHashMap  ->  {bob=9, cara=5, ann=3}    (correct)
collect into a plain toMap  ->  {ann=3, cara=5, bob=9}    (sort discarded)
```

## The reversed() trap (`SortingEdges`, item 5)

Sort by age descending, then name ascending:

```
comparingInt(age).thenComparing(name).reversed()   ->  [34:Dan, 34:Ann, ...]
comparingInt(age).reversed().thenComparing(name)   ->  [34:Ann, 34:Dan, ...]
```

`reversed()` applies to the whole chain built so far. Both compile, and the
difference only appears when two elements tie, which test data often avoids.

## Everything else that was verified

`EmployeeQueries` covers grouping by department (names, counts, totals,
averages), highest paid per department with and without the Optional,
second and third highest salary, top two per department, multi field sorting,
`summarizingInt`, `toMap` with a merge function and insertion order,
partitioning, and the department with the highest average.

`FrequencyAndDuplicates` covers frequency maps, duplicates two ways, most
frequent, distinct preserving order, character counting, anagrams, and words
appearing exactly twice.

`FlattenAndExceptions` covers list of lists, map of lists, nested records,
quantity totals per key, splitting sentences, `Optional::stream`, and the three
ways to handle a checked exception inside a lambda, including what the wrapped
failure actually looks like:

```
RuntimeException wrapping not a number: oops
```
