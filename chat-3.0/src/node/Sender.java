package node;

import node.messages.Message;
import node.messages.system.AckMessage;
import node.messages.system.HelloAckMessage;
import node.messages.system.HelloMessage;
import node.messages.user.UserMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

public class Sender {
    private final DatagramSocket datagramSocket;
    private final String nodeName;

    public Sender(DatagramSocket datagramSocket, String nodeName) {
        this.datagramSocket = datagramSocket;
        this.nodeName = nodeName;
    }

    private synchronized void sendMessage(Message message, SocketAddress destination) throws IOException {
        byte[] out = Util.serialize(message);
        DatagramPacket packet = new DatagramPacket(out, out.length, destination);
        datagramSocket.send(packet);
    }

    public void sendUserMessage(UserMessage userMessage, SocketAddress destination) throws IOException {
        sendMessage(userMessage, destination);
    }


    public void sendAck(Message message, SocketAddress source) throws IOException {
        Message ackMessage = new AckMessage(message.getUuid(), nodeName);
        sendMessage(ackMessage, source);
    }

    public void sendHelloAck(SocketAddress destination, SocketAddress myAlternate) throws IOException {
        Message helloAckMessage = new HelloAckMessage(myAlternate, nodeName);
        sendMessage(helloAckMessage, destination);
    }

    public void sendHello(SocketAddress neighbour, SocketAddress myAlternate) throws IOException {
        Message helloMessage = new HelloMessage(myAlternate, nodeName);
        sendMessage(helloMessage, neighbour);
    }

    public void sendPing(Message message, SocketAddress destination) throws IOException {
        sendMessage(message, destination);
    }
}
