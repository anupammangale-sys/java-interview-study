public class PostgresOrderRepository implements OrderRepository {
    public PostgresOrderRepository(String url) {
        System.out.println("  [db] opening connection to " + url);
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    public void savePayment(String orderId, String method, long feePaise) {
        System.out.println("  [db] saved " + orderId + " method=" + method
                + " fee=" + Money.rupees(feePaise));
    }
}
