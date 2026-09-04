import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

/**
 * The counting questions: duplicates, frequency, first non repeating.
 * Every answer was run and the printed output is what goes on the page.
 *
 *   java FrequencyAndDuplicates.java
 */
public class FrequencyAndDuplicates {

    static final List<String> WORDS =
            List.of("apple", "pear", "apple", "fig", "pear", "apple", "plum");
    static final String TEXT = "swiss cheese";

    public static void main(String[] args) {
        line("1. frequency of each word");
        show(WORDS.stream().collect(groupingBy(Function.identity(), counting())));

        line("   keeping the order the words first appeared");
        show(WORDS.stream().collect(groupingBy(Function.identity(),
                LinkedHashMap::new, counting())));

        line("2. words that appear more than once");
        show(WORDS.stream().collect(groupingBy(Function.identity(), counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList());

        line("   the same thing with a Set, which is the version to know");
        Set<String> seen = new HashSet<>();
        show(WORDS.stream().filter(w -> !seen.add(w)).collect(toSet()));

        line("3. the most frequent word");
        show(WORDS.stream().collect(groupingBy(Function.identity(), counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("none"));

        line("4. remove duplicates, keep the original order");
        show(WORDS.stream().distinct().toList());

        line("5. count each character in \"" + TEXT + "\", ignoring spaces");
        show(TEXT.chars()
                .filter(Character::isLetter)
                .mapToObj(c -> (char) c)
                .collect(groupingBy(Function.identity(), LinkedHashMap::new, counting())));

        line("6. first character that appears exactly once");
        show(TEXT.chars()
                .filter(Character::isLetter)
                .mapToObj(c -> (char) c)
                .collect(groupingBy(Function.identity(), LinkedHashMap::new, counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .findFirst().map(String::valueOf).orElse("none"));

        line("   why LinkedHashMap matters here: the same code with a plain HashMap");
        show(TEXT.chars()
                .filter(Character::isLetter)
                .mapToObj(c -> (char) c)
                .collect(groupingBy(Function.identity(), counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .findFirst().map(String::valueOf).orElse("none"));

        line("7. are two words anagrams");
        show(anagram("listen", "silent") + "  and  " + anagram("hello", "world"));

        line("8. words appearing exactly twice");
        show(WORDS.stream().collect(groupingBy(Function.identity(), counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() == 2)
                .map(Map.Entry::getKey)
                .toList());

        line("9. join the distinct words into one string");
        show(WORDS.stream().distinct().sorted().collect(joining(", ", "[", "]")));
    }

    private static boolean anagram(String a, String b) {
        return sorted(a).equals(sorted(b));
    }

    private static String sorted(String s) {
        return s.chars().sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static void line(String s) { System.out.println(s); }
    private static void show(Object o) { System.out.println("   -> " + o); System.out.println(); }
}
