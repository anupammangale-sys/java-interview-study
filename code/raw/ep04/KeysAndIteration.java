import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Two things that surprise people, both shown rather than described:
 *
 *  1. Changing a key after putting it in a map loses the entry permanently.
 *  2. Changing a collection while walking it. Some throw, some do not.
 *
 *   java KeysAndIteration.java
 */
public class KeysAndIteration {

    public static void main(String[] args) {
        System.out.println("=== 1. changing a key after putting it in ===");
        mutableKey();

        System.out.println();
        System.out.println("=== 2. changing a collection while walking it ===");
        whileIterating();

        System.out.println();
        System.out.println("=== 3. the same key object in a HashSet ===");
        mutableSetElement();
    }

    /** Mutable class whose identity depends on a field that can change. */
    static class Customer {
        String email;
        Customer(String email) { this.email = email; }
        @Override public boolean equals(Object o) {
            return o instanceof Customer c && c.email.equals(email);
        }
        @Override public int hashCode() { return email.hashCode(); }
        @Override public String toString() { return "Customer(" + email + ")"; }
    }

    private static void mutableKey() {
        Map<Customer, String> orders = new HashMap<>();
        Customer c = new Customer("anupam@x.com");
        orders.put(c, "order-1");

        System.out.println("before changing the email:");
        System.out.println("  get(c)      = " + orders.get(c));
        System.out.println("  size        = " + orders.size());

        c.email = "new@x.com";                 // the field hashCode() depends on

        System.out.println("after changing the email:");
        System.out.println("  get(c)      = " + orders.get(c) + "        <-- gone");
        System.out.println("  containsKey = " + orders.containsKey(c));
        System.out.println("  remove(c)   = " + orders.remove(c) + "        <-- cannot delete it either");
        System.out.println("  size        = " + orders.size() + "           <-- still there, taking memory");
        System.out.print("  iterating   = ");
        for (Map.Entry<Customer, String> e : orders.entrySet()) {
            System.out.print(e.getKey() + " -> " + e.getValue());
        }
        System.out.println("   <-- visible, but unreachable by key");
    }

    private static void mutableSetElement() {
        Set<Customer> set = new HashSet<>();
        Customer c = new Customer("a@x.com");
        set.add(c);
        c.email = "b@x.com";
        System.out.println("  contains(the very same object) = " + set.contains(c));
        System.out.println("  size                           = " + set.size());
    }

    private static void whileIterating() {
        // ArrayList, removing an early element: fails loudly
        try {
            List<String> list = new ArrayList<>(List.of("a", "b", "c", "d", "e"));
            for (String s : list) {
                if (s.equals("b")) list.remove(s);
            }
            System.out.println("  ArrayList, remove 'b' of 5 : no exception");
        } catch (Exception e) {
            System.out.println("  ArrayList, remove 'b' of 5 : " + e.getClass().getSimpleName()
                    + "   <-- fails fast, as expected");
        }

        // ArrayList, removing the second to last element: silently does NOT fail.
        // After the removal the cursor already equals the new size, so hasNext()
        // returns false and the check that would have thrown never runs.
        try {
            List<String> list = new ArrayList<>(List.of("a", "b", "c"));
            List<String> seen = new ArrayList<>();
            for (String s : list) {
                seen.add(s);
                if (s.equals("b")) list.remove(s);
            }
            System.out.println("  ArrayList, remove 'b' of 3 : no exception, but only saw " + seen
                    + "   <-- silently skipped 'c'");
        } catch (Exception e) {
            System.out.println("  ArrayList, remove 'b' of 3 : " + e.getClass().getSimpleName());
        }

        // The correct way to remove during a walk
        List<String> list2 = new ArrayList<>(List.of("a", "b", "c"));
        Iterator<String> it = list2.iterator();
        while (it.hasNext()) {
            if (it.next().equals("b")) it.remove();
        }
        System.out.println("  ArrayList via iterator: " + list2 + "        <-- the supported way");

        // CopyOnWriteArrayList: walks a snapshot, so no exception
        try {
            List<String> cow = new CopyOnWriteArrayList<>(List.of("a", "b", "c"));
            List<String> seen = new ArrayList<>();
            for (String s : cow) {
                seen.add(s);
                if (s.equals("b")) cow.remove(s);
            }
            System.out.println("  CopyOnWriteArrayList  : no exception, walked " + seen + ", list is now " + cow);
        } catch (Exception e) {
            System.out.println("  CopyOnWriteArrayList  : " + e.getClass().getSimpleName());
        }

        // ConcurrentHashMap: walks live data, no exception
        try {
            Map<String, Integer> m = new ConcurrentHashMap<>(Map.of("a", 1, "b", 2, "c", 3));
            int seen = 0;
            for (String k : m.keySet()) {
                seen++;
                m.remove(k);
            }
            System.out.println("  ConcurrentHashMap     : no exception, saw " + seen + " keys, map is now " + m);
        } catch (Exception e) {
            System.out.println("  ConcurrentHashMap     : " + e.getClass().getSimpleName());
        }
    }
}
