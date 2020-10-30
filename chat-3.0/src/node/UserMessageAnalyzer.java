package node;

import node.messages.Message;
import node.messages.system.AckMessage;
import node.messages.user.UserMessage;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserMessageAnalyzer implements Runnable {
    private final Map<Message, Set<SocketAddress>> unconfirmedMessageMap =
            Collections.synchronizedMap(new ConcurrentHashMap<>());
    private final Set<SocketAddress> neighbourSet = new HashSet<>();
    private final Deque<Message> incomingMessageDeque = new LinkedList<>();
    private final Sender sender;
    private final String nodeName;

    public UserMessageAnalyzer(Sender sender, String nodeName) {
        this.sender = sender;
        this.nodeName = nodeName;
    }

    public void deleteDestination(SocketAddress deadNeighbour) {
        unconfirmedMessageMap.forEach((key, value) -> {
            value.remove(deadNeighbour);
            if (value.isEmpty()) {
                unconfirmedMessageMap.remove(key);
            }
        });
        neighbourSet.remove(deadNeighbour);
    }

    public void changeDestination(SocketAddress deadNeighbour, SocketAddress neighbourAlternate) {
        unconfirmedMessageMap.forEach((key, value) -> {
            value.add(neighbourAlternate);
            value.remove(deadNeighbour);
        });
    }

    public void addNewNeighbour(SocketAddress source) {
        neighbourSet.add(source);
        unconfirmedMessageMap.forEach((message, socketAddresses) -> {
            socketAddresses.add(source);
        });
    }

    public void processMessageFromConsole(UserMessage message) {
        Set<SocketAddress> destinations = new HashSet<>(neighbourSet);
        unconfirmedMessageMap.put(message, destinations);
    }

    public void processMessage(Message message, SocketAddress source) {
        if (!incomingMessageDeque.contains(message)) {
            System.out.println(((UserMessage)message).getSourceName() + ": " + ((UserMessage)message).getText());
            Set<SocketAddress> destinations = new HashSet<>(neighbourSet);
            destinations.remove(source);
            unconfirmedMessageMap.put(message, destinations);
            incomingMessageDeque.add(message);
            if (incomingMessageDeque.size() > 100) {
                incomingMessageDeque.removeFirst();
            }
        }
        try {
            sender.sendAck(new AckMessage(message.getUuid(), nodeName), source);
        } catch (IOException e) {
            System.err.println("Unable to send message: " + e.getLocalizedMessage());
        }
    }

    public void confirmDelivery(Message message, SocketAddress source) {
        if (unconfirmedMessageMap.containsKey(message)) {
            Set<SocketAddress> destinationSet = unconfirmedMessageMap.get(message);
            destinationSet.remove(source);
            if (destinationSet.isEmpty()) {
                unconfirmedMessageMap.remove(message);
            }
            incomingMessageDeque.remove(message);
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            unconfirmedMessageMap.forEach((message, socketAddresses) -> {
                socketAddresses.forEach(destination -> {
                    try {
                        sender.sendUserMessage((UserMessage)message, destination);
                    } catch (IOException e) {
                        System.err.println("Unable to send message: " + e.getLocalizedMessage());
                        Thread.currentThread().interrupt();
                    }
                });
            });
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
