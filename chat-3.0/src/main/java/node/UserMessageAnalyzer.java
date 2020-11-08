package node;


import node.messages.Message;
import node.messages.system.AckMessage;
import node.messages.user.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserMessageAnalyzer implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(UserMessageAnalyzer.class);
    private final Map<Message, Set<SocketAddress>> unconfirmedMessageMap = new ConcurrentHashMap<>();
    private final Set<SocketAddress> neighbourSet = new HashSet<>();
    private final Sender sender;
    private final String nodeName;

    public UserMessageAnalyzer(Sender sender, String nodeName) {
        this.sender = sender;
        this.nodeName = nodeName;
    }

    public void deleteDestination(SocketAddress deadNeighbour) {
        neighbourSet.remove(deadNeighbour);
        unconfirmedMessageMap.forEach((key, value) -> {
            value.remove(deadNeighbour);
            if (value.isEmpty()) {
                unconfirmedMessageMap.remove(key);
            }
        });
        logger.debug("Delete destination; UMM: {}", unconfirmedMessageMap);
    }

    public void changeDestination(SocketAddress deadNeighbour, SocketAddress neighbourAlternate) {
        neighbourSet.remove(deadNeighbour);
        unconfirmedMessageMap.forEach((key, value) -> {
            if (value.contains(deadNeighbour)) {
                value.remove(deadNeighbour);
                value.add(neighbourAlternate);
            }
        });
        neighbourSet.add(neighbourAlternate);
        logger.debug("Change dest; UMM: {}", unconfirmedMessageMap);
    }

    public void addNewNeighbour(SocketAddress source) {
        if (!neighbourSet.contains(source)) {
            neighbourSet.add(source);
            unconfirmedMessageMap.forEach((message, socketAddresses) -> socketAddresses.add(source));
        }
        logger.debug("New neighbour: {}; UMM: {}", source, unconfirmedMessageMap);
    }

    public void processMessageFromConsole(UserMessage message) {
        Set<SocketAddress> destinations = new HashSet<>(neighbourSet);
        unconfirmedMessageMap.put(message, destinations);
        logger.debug("Message form console: {}; UMM: {}", message, unconfirmedMessageMap);
    }

    public void processMessage(UserMessage message, SocketAddress source) {
        if (!unconfirmedMessageMap.containsKey(message)) {
            System.out.println(message);
            Set<SocketAddress> destinations = new HashSet<>(neighbourSet);
            destinations.remove(source);
            if (!destinations.isEmpty())
                unconfirmedMessageMap.put(message, destinations);
        }
        try {
            sender.sendMessage(new AckMessage(message.getUuid(), nodeName), source);
        } catch (IOException e) {
            System.err.println("Unable to send message: " + e.getLocalizedMessage());
        }
        logger.debug("Message: {} received from {}; UMM: {}", message, source, unconfirmedMessageMap);
    }

    public void confirmDelivery(Message message, SocketAddress source) {
        if (unconfirmedMessageMap.containsKey(message)) {
            Set<SocketAddress> destinationSet = unconfirmedMessageMap.get(message);
            destinationSet.remove(source);
            if (destinationSet.isEmpty()) {
                unconfirmedMessageMap.remove(message);
            }
        }
        logger.debug("{}: delivery confirmed. UMM: {} ", message, unconfirmedMessageMap);

    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            unconfirmedMessageMap.forEach((message, socketAddresses) ->
                    socketAddresses.forEach(destination -> {
                                try {
                                    sender.sendMessage(message, destination);
                                } catch (IOException e) {
                                    System.err.println("Unable to send message: " + e.getLocalizedMessage());
                                    Thread.currentThread().interrupt();
                                }
                            }
                    ));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
