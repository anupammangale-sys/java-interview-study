import java.util.*;
import java.util.stream.Collectors;

/**
 * Sorting, including the two things that go wrong: nulls, and reversing the
 * wrong part of a chained comparator.
 *
 *   java SortingEdges.java
 */
public class SortingEdges {

    record Person(String name, String city, Integer age) {}

    static final List<Person> PEOPLE = Arrays.asList(
            new Person("Ann", "leeds", 34),
            new Person("Bob", "bath", null),
            new Person("Cara", "leeds", 29),
            new Person("Dan", "bath", 34),
            new Person("Eve", null, 22));

    public static void main(String[] args) {
        line("1. sort by age, with nulls last");
        show(PEOPLE.stream()
                .sorted(Comparator.comparing(Person::age, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(p -> p.name() + "=" + p.age())
                .toList());

        line("2. same, nulls first");
        show(PEOPLE.stream()
                .sorted(Comparator.comparing(Person::age, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(p -> p.name() + "=" + p.age())
                .toList());

        line("3. what happens with a plain comparator and a null value");
        try {
            PEOPLE.stream().sorted(Comparator.comparing(Person::age)).toList();
            show("no exception");
        } catch (NullPointerException e) {
            show("NullPointerException, because comparing() calls compareTo on null");
        }

        line("4. city then name, both ascending");
        show(PEOPLE.stream()
                .sorted(Comparator.comparing(Person::city, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Person::name))
                .map(p -> p.city() + ":" + p.name())
                .toList());

        line("5. the reversed() trap: this reverses the WHOLE chain");
        show(PEOPLE.stream()
                .filter(p -> p.age() != null)
                .sorted(Comparator.comparingInt(Person::age).thenComparing(Person::name).reversed())
                .map(p -> p.age() + ":" + p.name())
                .toList());

        line("   what people usually mean: age descending, then name ascending");
        show(PEOPLE.stream()
                .filter(p -> p.age() != null)
                .sorted(Comparator.comparingInt(Person::age).reversed().thenComparing(Person::name))
                .map(p -> p.age() + ":" + p.name())
                .toList());

        line("6. sorting a map by value, highest first, keeping the order");
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("ann", 3); scores.put("bob", 9); scores.put("cara", 5);
        show(scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new)));

        line("   the same without LinkedHashMap, which throws the order away");
        show(scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private static void line(String s) { System.out.println(s); }
    private static void show(Object o) { System.out.println("   -> " + o); System.out.println(); }
}
