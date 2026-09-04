import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Where a notification goes when it will never be delivered. */
public class DeadLetterQueue {
    public record Entry(Notification notification, String reason, int attempts) {}

    private final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());

    public void add(Notification n, String reason, int attempts) {
        entries.add(new Entry(n, reason, attempts));
    }
    public List<Entry> all() { return List.copyOf(entries); }
    public int size() { return entries.size(); }
}
