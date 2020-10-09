package Server;

import java.net.InetSocketAddress;
import java.util.*;

public class View implements Runnable {
    private static final Map<InetSocketAddress, Map.Entry<Double, Double>> speedStatistic
            = Collections.synchronizedMap(new HashMap<>());
    private static final Map<InetSocketAddress, String> statuses = Collections.synchronizedMap(new HashMap<>());
    private static final String OK_STATUS = "OK";
    private static final String ERROR_STATUS = "ERROR";

    public static synchronized void setStatuses(InetSocketAddress source, Exception e) {
        if (e != null) {
            statuses.put(source, ERROR_STATUS + ": " + e.getLocalizedMessage());
        } else {
            statuses.put(source, OK_STATUS);
        }
    }

    public static synchronized void setSpeedStatistic(InetSocketAddress source, Double currentSpeed, Double avgSpeed) {
        speedStatistic.put(source, new AbstractMap.SimpleEntry<>(currentSpeed, avgSpeed));
    }


    @Override
    public void run() {

    }
}
