package node.messages.system;

import java.net.SocketAddress;

public class HelloMessage extends SystemMessage {
    private final SocketAddress alternate;

    @Override
    public String toString() {
        return "Hello {" +
                "alternate=" + alternate +
                '}';
    }


    public HelloMessage(SocketAddress alternate, String sourceName) {
        super(sourceName);
        this.alternate = alternate;
    }

    public SocketAddress getAlternate() {
        return alternate;
    }
}
