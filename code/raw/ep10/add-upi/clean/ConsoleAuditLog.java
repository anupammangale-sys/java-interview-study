public class ConsoleAuditLog implements AuditLog {
    public void paymentTaken(String orderId, String method, String reference, long feePaise) {
        System.out.println("  [audit] " + orderId + " paid by " + method
                + " reference " + reference + " fee " + Money.rupees(feePaise));
    }
}
