package node.messages.system;

import node.messages.Message;

import java.util.UUID;

public class SystemMessage extends Message {
    private final String sourceName;

    protected SystemMessage(String sourceName) {
        super();
        this.sourceName = sourceName;
    }

    protected SystemMessage(UUID uuid, String sourceName) {
        super(uuid);
        this.sourceName = sourceName;
    }

    public String getSourceName() {
        return sourceName;
    }
}
