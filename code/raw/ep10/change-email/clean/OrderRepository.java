public interface OrderRepository {
    void savePayment(String orderId, String method, long feePaise);
}
