import java.io.*;
import java.lang.reflect.Constructor;

/**
 * Three ways to break a Singleton, and what actually resists them.
 * Everything here was run: the "broken" results are real, not claimed.
 *
 *   java SingletonBreaking.java
 */
public class SingletonBreaking {

    // ---- the textbook Singleton ----
    static class Classic implements Serializable, Cloneable {
        private static final Classic INSTANCE = new Classic();
        private Classic() {}
        static Classic getInstance() { return INSTANCE; }
        @Override public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    // ---- the same thing, defended ----
    static class Defended implements Serializable {
        private static final Defended INSTANCE = new Defended();
        private Defended() {
            if (INSTANCE != null) {                     // stops reflection
                throw new IllegalStateException("already created, use getInstance()");
            }
        }
        static Defended getInstance() { return INSTANCE; }
        private Object readResolve() {                  // stops serialization
            return INSTANCE;
        }
    }

    // ---- the enum ----
    enum Enum1 {
        INSTANCE;
        int value = 42;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. reflection against a classic singleton ===");
        Classic a = Classic.getInstance();
        Constructor<Classic> c = Classic.class.getDeclaredConstructor();
        c.setAccessible(true);                          // private no longer matters
        Classic b = c.newInstance();
        report("Classic", a, b);

        System.out.println("=== 2. serialization against a classic singleton ===");
        Classic d = roundTrip(Classic.getInstance());
        report("Classic", Classic.getInstance(), d);

        System.out.println("=== 3. cloning against a classic singleton ===");
        Classic e = (Classic) Classic.getInstance().clone();
        report("Classic", Classic.getInstance(), e);

        System.out.println("=== 4. the same attacks on the defended version ===");
        try {
            Constructor<Defended> dc = Defended.class.getDeclaredConstructor();
            dc.setAccessible(true);
            Defended extra = dc.newInstance();
            report("Defended", Defended.getInstance(), extra);
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            System.out.println("  reflection    -> blocked: " + cause.getClass().getSimpleName()
                    + ": " + cause.getMessage());
        }
        Defended ds = roundTrip(Defended.getInstance());
        System.out.println("  serialization -> same instance? "
                + (Defended.getInstance() == ds) + "   (readResolve did this)");
        System.out.println();

        System.out.println("=== 5. the same attacks on an enum ===");
        try {
            Constructor<?>[] ctors = Enum1.class.getDeclaredConstructors();
            Constructor<?> ec = ctors[0];
            ec.setAccessible(true);
            ec.newInstance("EXTRA", 1);
            System.out.println("  reflection    -> a second enum constant was created");
        } catch (Exception ex) {
            System.out.println("  reflection    -> blocked: " + ex.getClass().getSimpleName()
                    + ": " + ex.getMessage());
        }
        Enum1 es = roundTrip(Enum1.INSTANCE);
        System.out.println("  serialization -> same instance? " + (Enum1.INSTANCE == es)
                + "   (the language guarantees this)");
        System.out.println("  cloning       -> not possible, enums cannot be cloned");
    }

    private static void report(String label, Object a, Object b) {
        System.out.println("  same instance? " + (a == b)
                + "   hash " + Integer.toHexString(System.identityHashCode(a))
                + " vs " + Integer.toHexString(System.identityHashCode(b)));
        System.out.println();
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T obj) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(obj);
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
