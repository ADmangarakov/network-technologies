package com.github.ADmangarakov;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class View implements Runnable {
    private static View view;

    public static View getInstance() {
        if (view == null)
            view = new View();
        return view;
    }

    private final int TTL = 5000;
    private final Map<InetAddress, Date> clones;

    private View() {
        clones = Collections.synchronizedMap(new HashMap<>());
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            List<InetAddress> forRemove = new LinkedList<>();
            clones.forEach((inetAddress, date) -> {
                if (System.currentTimeMillis() - date.getTime() < TTL) {
                    try {
                        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                    } catch (InterruptedException | IOException e) {
                        System.out.println("Can't clear the console!");
                        System.out.println("------------------------------");
                    }
                    System.out.println("Online: " + inetAddress);
                }
                else {
                    System.out.println("Offline: " + inetAddress);
                    forRemove.add(inetAddress);
                }
            });
            forRemove.forEach(clones::remove);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                System.err.println("View interrupted!");
                Thread.currentThread().interrupt();
            }
        }
    }

    public Map<InetAddress, Date> getClones() {
        return clones;
    }
}
