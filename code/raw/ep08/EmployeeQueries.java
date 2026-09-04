import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * The employee questions, which is what most stream coding rounds actually ask.
 * Every answer here was run, and the printed output is what goes on the page.
 *
 *   java EmployeeQueries.java
 */
public class EmployeeQueries {

    record Employee(String name, String dept, int salary, int age) {}

    static final List<Employee> STAFF = List.of(
            new Employee("Ann",   "eng",   120, 34),
            new Employee("Bob",   "eng",   100, 28),
            new Employee("Cara",  "eng",   140, 41),
            new Employee("Dan",   "sales",  90, 25),
            new Employee("Eve",   "sales", 110, 37),
            new Employee("Finn",  "hr",     80, 45),
            new Employee("Gita",  "hr",     95, 30));

    public static void main(String[] args) {
        line("1. names grouped by department");
        show(STAFF.stream().collect(
                groupingBy(Employee::dept, mapping(Employee::name, toList()))));

        line("2. how many in each department");
        show(STAFF.stream().collect(groupingBy(Employee::dept, counting())));

        line("3. average salary per department");
        show(STAFF.stream().collect(groupingBy(Employee::dept, averagingInt(Employee::salary))));

        line("4. total salary per department");
        show(STAFF.stream().collect(groupingBy(Employee::dept, summingInt(Employee::salary))));

        line("5. highest paid person in each department");
        Map<String, Optional<Employee>> topOptional = STAFF.stream().collect(
                groupingBy(Employee::dept, maxBy(Comparator.comparingInt(Employee::salary))));
        show(topOptional.entrySet().stream().collect(toMap(Map.Entry::getKey,
                e -> e.getValue().map(Employee::name).orElse("none"))));

        line("   the same thing without the Optional, using collectingAndThen");
        show(STAFF.stream().collect(groupingBy(Employee::dept,
                collectingAndThen(maxBy(Comparator.comparingInt(Employee::salary)),
                        o -> o.map(Employee::name).orElse("none")))));

        line("6. second highest salary overall");
        show(STAFF.stream().map(Employee::salary).distinct()
                .sorted(Comparator.reverseOrder())
                .skip(1).findFirst().orElse(-1));

        line("   third highest, same shape with skip(2)");
        show(STAFF.stream().map(Employee::salary).distinct()
                .sorted(Comparator.reverseOrder())
                .skip(2).findFirst().orElse(-1));

        line("7. top 2 earners in each department");
        show(STAFF.stream().collect(groupingBy(Employee::dept,
                collectingAndThen(toList(), list -> list.stream()
                        .sorted(Comparator.comparingInt(Employee::salary).reversed())
                        .limit(2)
                        .map(Employee::name)
                        .toList()))));

        line("8. sort by department, then by salary highest first");
        show(STAFF.stream()
                .sorted(Comparator.comparing(Employee::dept)
                        .thenComparing(Comparator.comparingInt(Employee::salary).reversed()))
                .map(e -> e.dept() + ":" + e.name() + ":" + e.salary())
                .toList());

        line("9. one summary pass: count, min, max, average, total");
        show(STAFF.stream().collect(summarizingInt(Employee::salary)));

        line("10. names to salaries as a map, keeping insertion order");
        show(STAFF.stream().collect(toMap(Employee::name, Employee::salary,
                (a, b) -> a, LinkedHashMap::new)));

        line("11. partition into high and low earners");
        show(STAFF.stream().collect(partitioningBy(e -> e.salary() >= 100,
                mapping(Employee::name, toList()))));

        line("12. department with the highest average salary");
        show(STAFF.stream()
                .collect(groupingBy(Employee::dept, averagingInt(Employee::salary)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("none"));
    }

    private static void line(String s) { System.out.println(s); }
    private static void show(Object o) { System.out.println("   -> " + o); System.out.println(); }
}
