public class PushChannel implements Channel {
    public String name() { return "push"; }

    public void send(Notification n) {
        System.out.println("      [push] delivered to device " + n.recipient());
    }
}
