package node.messages.system;

import java.net.SocketAddress;

public class HelloAckMessage extends HelloMessage {

    @Override
    public String toString() {
        return "Hello {" +
                "alternate=" + getAlternate() +
                '}';
    }

    public HelloAckMessage(SocketAddress alternate, String sourceName) {
        super(alternate, sourceName);
    }
}
