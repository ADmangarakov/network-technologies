package node;


import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Node {
    private final int port;
    private final String name;
    private final int packetLoss;
    private SocketAddress alternate;

    public Node(String name, int port, SocketAddress neighbour, int packetLoss) {
        this.name = name;
        this.port = port;
        this.packetLoss = packetLoss;
        if (neighbour != null) {
            alternate = neighbour;
        }
    }

    public Node(String name, int port, int packetLoss) throws SocketException {
        this(name, port, null, packetLoss);
    }

    public void start() {
        Receiver receiver;
        ConsoleReceiver consoleReceiver;
        Sender sender;
        SystemMessageAnalyzer systemMessageAnalyzer;
        UserMessageAnalyzer userMessageAnalyzer;
        DatagramSocket datagramSocket;
        try {
            datagramSocket = new DatagramSocket(port, InetAddress.getLoopbackAddress());
            sender = new Sender(datagramSocket);
            userMessageAnalyzer = new UserMessageAnalyzer(sender, name);
            if (alternate != null) {
                systemMessageAnalyzer = new SystemMessageAnalyzer(sender, userMessageAnalyzer,
                        datagramSocket.getLocalSocketAddress(), name);

                systemMessageAnalyzer.sendHello(alternate);
            } else {
                systemMessageAnalyzer = new SystemMessageAnalyzer(sender, name, userMessageAnalyzer, datagramSocket.getLocalSocketAddress());
            }

            receiver = new Receiver(datagramSocket, packetLoss, systemMessageAnalyzer, userMessageAnalyzer, sender);
            consoleReceiver = new ConsoleReceiver(name, userMessageAnalyzer);
        } catch (SocketException e) {
            System.err.println("Unable to create receiver: " + e.getLocalizedMessage());
            return;
        } catch (IOException e) {
            System.err.println("Unable to send hello: " + e.getLocalizedMessage());
            return;
        }
        Thread receiverThread = new Thread(receiver);
        Thread consoleReceiverThread = new Thread(consoleReceiver);
        Thread systemMessageAnalyzerThread = new Thread(systemMessageAnalyzer);
        Thread userMessageAnalyzerThread = new Thread(userMessageAnalyzer);
        receiverThread.start();
        consoleReceiverThread.start();
        systemMessageAnalyzerThread.start();
        userMessageAnalyzerThread.start();

        while (receiverThread.isAlive() && consoleReceiverThread.isAlive()
                && systemMessageAnalyzerThread.isAlive() && userMessageAnalyzerThread.isAlive()) ;
        receiverThread.interrupt();
        consoleReceiverThread.interrupt();
        systemMessageAnalyzerThread.interrupt();
        userMessageAnalyzerThread.interrupt();
        try {
            receiverThread.join();
            consoleReceiverThread.join();
            systemMessageAnalyzerThread.join();
            userMessageAnalyzerThread.join();
        } catch (InterruptedException e) {
            System.err.println("Unable to finish: " + e.getLocalizedMessage());
        }
        datagramSocket.close();
    }

    public static void main(String[] args) {
        Node node;
        try {
            if (args.length == 3) {
                node = new Node(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            } else if (args.length == 5) {
                int neighbourPort = Integer.parseInt(args[4]);
                InetAddress neighbourInetAddress = InetAddress.getByName(args[3]);
                node = new Node(args[0], Integer.parseInt(args[1]),
                        new InetSocketAddress(neighbourInetAddress, neighbourPort), Integer.parseInt(args[2]));
            } else {
                System.err.println("Wrong arguments");
                return;
            }
        } catch (UnknownHostException e) {
            System.err.println("Can't parse host name: " + e.getLocalizedMessage());
            return;
        } catch (NumberFormatException e) {
            System.err.println("Can't parse arguments name: " + e.getLocalizedMessage());
            return;
        } catch (SocketException e) {
            System.err.println("Can't create node: " + e.getLocalizedMessage());
            return;
        }

        node.start();
    }
}
