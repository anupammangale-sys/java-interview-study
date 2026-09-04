/** This feature asks for exactly what it needs and nothing more. */
public class SplitTheBill {
    public static String run(Splittable method, int people, long amountPaise) {
        return method.splitAcross(people, amountPaise);
    }
}
