public class Main {
    public static void main(String[] args) {
        OrderService service = new OrderService();
        service.checkout(new Order("A-1001", 250000, "asha@example.com"), "card");
        System.out.println();
        service.checkout(new Order("A-1002", 899900, "ravi@example.com"), "netbanking");
    }
}
