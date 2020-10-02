package com.github.ADmangarakov;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.*;

public class View implements Runnable {
    private static View view;

    public static View getInstance() {
        if (view == null)
            view = new View();
        return view;
    }

    private final int TTL = 5000;
    private final Map<SocketAddress, Date> clones;

    private View() {
        clones = Collections.synchronizedMap(new HashMap<>());
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            List<SocketAddress> forRemove = new LinkedList<>();
            clones.forEach((socketAddress, date) -> {
                if (System.currentTimeMillis() - date.getTime() < TTL) {
                    System.out.println("Online: " + socketAddress);
                } else {
                    System.out.println("Offline: " + socketAddress);
                    forRemove.add(socketAddress);
                }
            });
            forRemove.forEach(clones::remove);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.err.println("View interrupted!");
                Thread.currentThread().interrupt();
            } finally {
                try {
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                } catch (InterruptedException | IOException e) {
                    System.out.println("Can't clear the console!");
                    System.out.println("------------------------------");
                }
            }
        }
    }

    public Map<SocketAddress, Date> getClones() {
        return clones;
    }
}
