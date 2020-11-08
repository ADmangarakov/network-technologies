package node.messages.system;

import node.messages.Message;

import java.util.Objects;
import java.util.UUID;

public class AckMessage extends SystemMessage {
    public AckMessage(UUID uuid, String sourceName) {
        super(uuid, sourceName);
    }

    @Override
    public String toString() {
        return "AckMessage " + getSourceName() + " : " + getUuid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return Objects.equals(getUuid(), message.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid());
    }

}
