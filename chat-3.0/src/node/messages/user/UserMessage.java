package node.messages.user;

import node.messages.Message;

import java.util.Objects;

public class UserMessage extends Message {
    private final String sourceName;
    private final String text;

    public UserMessage(String text, String sourceName) {
        super();
        this.sourceName = sourceName;
        this.text = text;
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

    public String getSourceName() {
        return sourceName;
    }

    public String getText() {
        return text;
    }
}
