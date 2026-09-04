/** Stands in for a real database. Note that it is opened, not handed in. */
public class Database {
    public static Database connect(String url) {
        System.out.println("  [db] opening connection to " + url);
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return new Database();
    }
    public void savePayment(String orderId, String method, long fee) {
        System.out.println("  [db] saved " + orderId + " method=" + method + " fee=" + Money.rupees(fee));
    }
}
