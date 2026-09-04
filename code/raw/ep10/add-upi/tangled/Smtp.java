/** Stands in for a real mail server. Also opened, not handed in. */
public class Smtp {
    public static Smtp connect(String host) {
        System.out.println("  [smtp] opening connection to " + host);
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return new Smtp();
    }
    public void send(String to, String subject, String body) {
        System.out.println("  [smtp] to " + to + " subject: " + subject);
        System.out.println("  [smtp] body: " + body);
    }
}
