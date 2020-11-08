package node;

import node.messages.system.AckMessage;
import node.messages.system.HelloAckMessage;
import node.messages.system.HelloMessage;
import node.messages.system.PingMessage;
import node.messages.system.SystemMessage;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SystemMessageAnalyzer implements Runnable {
    private final Map<SocketAddress, Long> neighbourPingTimeMap = new ConcurrentHashMap<>();
    private final Map<SocketAddress, SocketAddress> neighbourAlternateMap = new HashMap<>();
    private final Sender sender;
    private final String nodeName;
    private final UserMessageAnalyzer userMessageAnalyzer;
    private final SocketAddress mySocketAddress;
    private SocketAddress myAlternate;

    private final int TIMEOUT = 8000;

    @Override
    public void run() {
        Map<SocketAddress, Long> neighbourForRemove = new HashMap<>();
        while (!Thread.currentThread().isInterrupted()) {
            neighbourForRemove.clear();
            neighbourPingTimeMap.forEach((key, value) -> {
                if (System.currentTimeMillis() - value > TIMEOUT) {
                    System.err.println(key + " doesn't response");
                    neighbourForRemove.put(key, value);
                }
                try {
                    sender.sendMessage(new PingMessage(nodeName), key);
                } catch (IOException e) {
                    System.err.println("Unable to send ping: " + e.getLocalizedMessage());
                }
            });

            neighbourForRemove.forEach((key, value) -> {
                neighbourPingTimeMap.remove(key);
                rebuild(key);
            });
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    private void rebuild(SocketAddress deadNeighbour) {
        SocketAddress neighbourAlternate = neighbourAlternateMap.get(deadNeighbour);
        neighbourAlternateMap.remove(deadNeighbour);
        if (deadNeighbour.equals(myAlternate)) {
            myAlternate = neighbourAlternate;
            if (neighbourAlternate.equals(mySocketAddress)) {
                myAlternate = neighbourAlternateMap.keySet().stream().findAny().orElse(null);
            }
            neighbourAlternateMap.keySet().forEach(neighbour -> {
                try {
                    sender.sendMessage(new HelloMessage(myAlternate, nodeName), neighbour);
                } catch (IOException e) {
                    System.err.println("Unable to send message!");
                    Thread.currentThread().interrupt();
                    return;
                }
            });
        }

        if (neighbourAlternate.equals(mySocketAddress)) {
            userMessageAnalyzer.deleteDestination(deadNeighbour);
        } else {
            userMessageAnalyzer.changeDestination(deadNeighbour, neighbourAlternate);
            try {
                sender.sendMessage(new HelloMessage(myAlternate, nodeName), neighbourAlternate);
            } catch (IOException e) {
                System.out.println("Unable to send message: " + e.getLocalizedMessage());
            }
        }
    }

    public SystemMessageAnalyzer(Sender sender, String nodeName, UserMessageAnalyzer userMessageAnalyzer, SocketAddress mySocketAddress) {
        this.nodeName = nodeName;
        this.sender = sender;
        this.userMessageAnalyzer = userMessageAnalyzer;
        this.mySocketAddress = mySocketAddress;
    }

    public SystemMessageAnalyzer(Sender sender, UserMessageAnalyzer userMessageAnalyzer, SocketAddress mySocketAddress,
                                 String nodeName) {
        this(sender, nodeName, userMessageAnalyzer, mySocketAddress);
    }

    public void sendHello(SocketAddress destination) throws IOException {
        sender.sendMessage(new HelloMessage(destination, nodeName), destination);
    }

    public void processMessage(SystemMessage systemMessage, SocketAddress source) {
        if (systemMessage instanceof PingMessage) {
            if (neighbourAlternateMap.containsKey(source)) {
                neighbourPingTimeMap.put(source, System.currentTimeMillis());
            } else {
                System.err.println("Unknown source: " + source);
            }
        } else if (systemMessage instanceof HelloMessage) {
            try {
                if (!(systemMessage instanceof HelloAckMessage)) {
                    sender.sendMessage(new HelloAckMessage(myAlternate, nodeName), source);
                }
            } catch (IOException e) {
                System.err.println("Unable to send message!");
            }
            HelloMessage helloMessage = (HelloMessage) systemMessage;
            if (helloMessage.getAlternate() != null) {
                neighbourAlternateMap.put(source, helloMessage.getAlternate());
            }
            else {
                neighbourAlternateMap.put(source, mySocketAddress);
            }
            userMessageAnalyzer.addNewNeighbour(source);
            neighbourPingTimeMap.put(source, System.currentTimeMillis());
            if (myAlternate == null) {
                myAlternate = source;
            }
            System.out.println(systemMessage + " from " + systemMessage.getSourceName()
                    + "; My alternate is " + myAlternate);
        } else if (systemMessage instanceof AckMessage) {
            userMessageAnalyzer.confirmDelivery(systemMessage, source);
        }
    }
}
