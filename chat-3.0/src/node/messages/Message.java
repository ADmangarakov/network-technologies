package node.messages;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public abstract class Message implements Serializable {
    private final UUID uuid;

    protected Message(UUID uuid) {
        this.uuid = uuid;
    }

    protected Message() {
        uuid = UUID.randomUUID();
    }

    public UUID getUuid() {
        return uuid;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return Objects.equals(uuid, message.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

}
