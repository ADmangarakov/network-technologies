package com.github.ADmangarakov;

import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.Map;

public class Receiver implements Runnable {
    private final MulticastSocket multicastSocket;
    private final InetAddress groupAddr;
    private DatagramPacket packet;
    private final Map<InetAddress, Date> clones;

    public Receiver(String groupInetAddrName, Map<InetAddress, Date> clones) throws IOException {
        groupAddr = InetAddress.getByName(groupInetAddrName);
        this.clones = clones;

        multicastSocket = new MulticastSocket(8080);
        multicastSocket.joinGroup(
                new InetSocketAddress(groupAddr, 8080),
                NetworkInterface.getByInetAddress(groupAddr)
        );
        byte[] incMsg = new byte[256];
        packet = new DatagramPacket(incMsg, incMsg.length);
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                multicastSocket.receive(packet);
            } catch (IOException e) {
                System.err.println("Unable to receive msg: " + e.getLocalizedMessage());
                System.err.println("Shutdown.");
                Thread.currentThread().interrupt();
                break;
            }
            clones.merge(packet.getAddress(), new Date(System.currentTimeMillis()), (o, n) -> n);
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
