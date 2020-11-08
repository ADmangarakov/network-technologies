package node;

import node.messages.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

public class Sender {
    private final DatagramSocket datagramSocket;

    public Sender(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    public synchronized void sendMessage(Message message, SocketAddress destination) throws IOException {
        byte[] out = Util.serialize(message);
        DatagramPacket packet = new DatagramPacket(out, out.length, destination);
        datagramSocket.send(packet);
    }
}
