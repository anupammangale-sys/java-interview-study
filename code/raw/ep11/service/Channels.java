import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The factory. Callers ask for a channel by name and never write
 * "new EmailChannel()" themselves, so the set of channels can grow without
 * any caller changing.
 */
public class Channels {
    private final Map<String, Channel> byName = new LinkedHashMap<>();

    public Channels(List<Channel> channels) {
        for (Channel c : channels) byName.put(c.name(), c);
    }

    public Channel get(String name) {
        Channel c = byName.get(name);
        if (c == null) throw new IllegalArgumentException("no such channel: " + name);
        return c;
    }

    public java.util.Set<String> names() { return byName.keySet(); }
}
