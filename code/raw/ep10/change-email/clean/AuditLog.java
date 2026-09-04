public interface AuditLog {
    void paymentTaken(String orderId, String method, String reference, long feePaise);
}
