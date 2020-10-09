package Server;

import javax.swing.text.html.parser.Entity;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.*;

import static java.lang.Thread.sleep;

public class View implements Runnable {
    private static final Map<InetSocketAddress, Map.Entry<BigDecimal, BigDecimal>> speedStatistic
            = Collections.synchronizedMap(new HashMap<>());
    private static final Map<InetSocketAddress, String> statuses = Collections.synchronizedMap(new HashMap<>());
    private static final List<InetSocketAddress> forDelete = Collections.synchronizedList(new LinkedList<>());
    private static final String OK_STATUS = "OK";
    private static final String FINISHED_STATUS = "FINISHED";
    private static final String ERROR_STATUS = "ERROR";
    private static View view;

    private View() {
    }

    public static View getInstance() {
        return view == null ? new View() : view;
    }

    public static synchronized void setStatuses(InetSocketAddress source, Exception e) {
        statuses.put(source, ERROR_STATUS + ": " + e.getLocalizedMessage());
        forDelete.add(source);
    }

    public static synchronized void setStatuses(InetSocketAddress source) {
        statuses.put(source, OK_STATUS);
    }

    public static synchronized void setFinishedStatus(InetSocketAddress source) {
        statuses.put(source, FINISHED_STATUS);
        forDelete.add(source);
    }

    public static synchronized void setStatuses(InetSocketAddress source, String msg) {
        statuses.put(source, msg);
    }

    public static synchronized void setSpeedStatistic(InetSocketAddress source, BigDecimal currentSpeed, BigDecimal avgSpeed) {
        speedStatistic.put(source, new AbstractMap.SimpleEntry<>(currentSpeed, avgSpeed));
    }


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (!speedStatistic.isEmpty()) {
                System.out.println("-------------------------------->");
                for (Map.Entry aliveConnection : speedStatistic.entrySet()) {
                    Map.Entry speeds = ((Map.Entry) aliveConnection.getValue());
                    System.out.println();
                    System.out.println("Client: " + aliveConnection.getKey() +
                            ", current speed: " + speeds.getKey() + "bps" +
                            ", avgSpeed: " + speeds.getValue() + "bps" +
                            ", status: " + statuses.get(aliveConnection.getKey()));
                    System.out.println();
                }
                System.out.println("<--------------------------------");
            }
            for (InetSocketAddress disconnectedClient : forDelete) {
                statuses.remove(disconnectedClient);
                speedStatistic.remove(disconnectedClient);
            }
            try {
                sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
