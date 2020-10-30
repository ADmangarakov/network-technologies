package node.messages.system;

import java.util.UUID;

public class PingMessage extends SystemMessage {

    public PingMessage(String sourceName) {
        super(sourceName);
    }

    public PingMessage(UUID uuid, String sourceName) {
        super(uuid, sourceName);
    }

    @Override
    public String toString() {
        return "Ping";
    }
}
