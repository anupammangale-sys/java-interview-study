public class EmailChannel implements Channel {
    public String name() { return "email"; }

    public void send(Notification n) throws NotificationException {
        if (!n.recipient().contains("@")) {
            // wrong today and wrong in ten minutes, so never retry this
            throw new InvalidRecipientException("not an email address: " + n.recipient());
        }
        System.out.println("      [email] delivered to " + n.recipient());
    }
}
