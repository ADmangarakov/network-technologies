package node;

import node.messages.system.AckMessage;
import node.messages.system.HelloAckMessage;
import node.messages.system.HelloMessage;
import node.messages.system.PingMessage;
import node.messages.system.SystemMessage;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SystemMessageAnalyzer implements Runnable {
    private final Map<SocketAddress, Long> neighbourPingTimeMap = Collections.synchronizedMap(new ConcurrentHashMap<>());
    private final Map<SocketAddress, SocketAddress> neighbourAlternateMap;
    private final Sender sender;
    private final String nodeName;
    private final UserMessageAnalyzer userMessageAnalyzer;
    private final SocketAddress mySocketAddress;
    private SocketAddress myAlternate;
    private String myAlternateName;


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Map<SocketAddress, Long> neighbourForRemove = new HashMap<>();
            neighbourPingTimeMap.forEach((key, value) -> {
                if (System.currentTimeMillis() - value > 10000) {
                    System.out.println(key + " doesn't response");
                    neighbourForRemove.put(key, value);
                }
                try {
                    sender.sendPing(new PingMessage(nodeName), key);
                } catch (IOException e) {
                    System.err.println("Unable to send ping: " + e.getLocalizedMessage());
                }
            });

            neighbourForRemove.forEach((key, value) -> {
                neighbourPingTimeMap.remove(key);
                rebuild(key);
            });
            try {
                Thread.sleep(500);
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
                    sender.sendHello(neighbour, myAlternate);
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
                sender.sendHello(neighbourAlternate, myAlternate);
            } catch (IOException e) {
                System.out.println("Unable to send message: " + e.getLocalizedMessage());
            }
        }
    }

    public SystemMessageAnalyzer(Sender sender, String nodeName, UserMessageAnalyzer userMessageAnalyzer, SocketAddress mySocketAddress) {
        this.nodeName = nodeName;
        this.neighbourAlternateMap = new HashMap<>();
        this.sender = sender;
        this.userMessageAnalyzer = userMessageAnalyzer;
        this.mySocketAddress = mySocketAddress;
    }

    public SystemMessageAnalyzer(Sender sender, UserMessageAnalyzer userMessageAnalyzer, SocketAddress mySocketAddress,
                                 String nodeName, SocketAddress myAlternate) {
        this(sender, nodeName, userMessageAnalyzer, mySocketAddress);
//        this.myAlternate = myAlternate;
    }

    public void processMessage(SystemMessage systemMessage, SocketAddress source) {
        if (systemMessage instanceof PingMessage) {
//            System.out.println(systemMessage + "from " + source);
            if (neighbourAlternateMap.containsKey(source)) {
                neighbourPingTimeMap.put(source, System.currentTimeMillis());
            } else {
                System.err.println("Unknown source: " + source);
            }
        } else if (systemMessage instanceof HelloMessage) {
            try {
                if (!(systemMessage instanceof HelloAckMessage)) {
                    sender.sendHelloAck(source, myAlternate);
                }
            } catch (IOException e) {
                System.err.println("Unable to send message!");
            }
            System.out.println(systemMessage + " from " + systemMessage.getSourceName());
            HelloMessage helloMessage = (HelloMessage) systemMessage;
            if (helloMessage.getAlternate() != null)
                neighbourAlternateMap.put(source, helloMessage.getAlternate());
            else
                neighbourAlternateMap.put(source, mySocketAddress);
            userMessageAnalyzer.addNewNeighbour(source);
            neighbourPingTimeMap.put(source, System.currentTimeMillis());
            if (myAlternate == null) {
                myAlternate = source;
                myAlternateName = systemMessage.getSourceName();
            }
            System.out.println("My alternate is " + myAlternate);
        } else if (systemMessage instanceof AckMessage) {
            userMessageAnalyzer.confirmDelivery(systemMessage, source);
        }
    }


}
