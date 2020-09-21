package com.github.ADmangarakov;

import java.io.IOException;
import java.net.*;

public class Sender implements Runnable {
    private final MulticastSocket multicastSocket;
    private final DatagramPacket packet;
    private final String MSG = "Hello there!";
    private final InetAddress groupAddr;
    private final Integer port;
    private final static int MAX_ATTEMPT = 5;

    public Sender(String groupInetAddrName, Integer port) throws IOException {
        this.groupAddr = InetAddress.getByName(groupInetAddrName);
        this.port = port;

        multicastSocket = new MulticastSocket();

        byte[] msg = MSG.getBytes();
        packet = new DatagramPacket(msg, msg.length, groupAddr, port);
    }

    public void run() {
        int failedAttempts = 0;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                multicastSocket.send(packet);
                failedAttempts = 0;
            } catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
                failedAttempts++;
                if (failedAttempts == MAX_ATTEMPT) {
                    System.err.println("Unable to send message. Shutdown.");
                    Thread.currentThread().interrupt();
                    break;
                }
                System.out.println("Unable to send message: "+ e.getLocalizedMessage());
                System.out.println("Tries " + (MAX_ATTEMPT - failedAttempts) + " more times.");
            }
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            multicastSocket.leaveGroup(
                    new InetSocketAddress(groupAddr, 8080),
                    NetworkInterface.getByInetAddress(groupAddr)
            );
        } catch (IOException e) {
            System.err.println(Thread.currentThread().getName() + ": unable leave the group: " + e.getLocalizedMessage());
        }
        multicastSocket.close();
    }
}
