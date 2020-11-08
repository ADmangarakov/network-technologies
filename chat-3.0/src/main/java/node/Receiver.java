package node;

import node.messages.Message;
import node.messages.system.SystemMessage;
import node.messages.user.UserMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Random;

import static node.Util.deserialize;

public class Receiver implements Runnable {
    private final DatagramSocket datagramSocket;
    private final int packetLoss;
    private final byte[] buff = new byte[4096];
    private final SystemMessageAnalyzer systemMessageAnalyzer;
    private final UserMessageAnalyzer userMessageAnalyzer;
    protected final Sender sender;

    public Receiver(DatagramSocket datagramSocket, int packetLoss,
                    SystemMessageAnalyzer systemMessageAnalyzer, UserMessageAnalyzer userMessageAnalyzer, Sender sender) throws SocketException {
        this.datagramSocket = datagramSocket;
        this.packetLoss = packetLoss;
        this.systemMessageAnalyzer = systemMessageAnalyzer;
        this.userMessageAnalyzer = userMessageAnalyzer;
        this.sender = sender;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            DatagramPacket packet = new DatagramPacket(buff, buff.length);
            try {
                datagramSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Message msg = (Message) deserialize(packet.getData());
                processMessage(msg, packet.getSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        datagramSocket.close();
    }

    private void processMessage(Message message, SocketAddress source) {
        if (message instanceof SystemMessage) {
            systemMessageAnalyzer.processMessage((SystemMessage) message, source);
        } else {
            if (new Random().nextInt(100) >= packetLoss) {
                userMessageAnalyzer.processMessage((UserMessage)message, source);
            }
        }
    }
}
