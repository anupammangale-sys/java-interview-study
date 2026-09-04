/** Money is held in paise, never in double. */
public final class Money {
    private Money() {}
    public static String rupees(long paise) {
        return String.format("Rs %d.%02d", paise / 100, Math.abs(paise % 100));
    }
}
