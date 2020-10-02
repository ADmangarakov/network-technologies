package com.github.ADmangarakov;

import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.Map;

public class Receiver implements Runnable {
    private final MulticastSocket multicastSocket;
    private final InetAddress groupAddr;
    private DatagramPacket packet;
    private final Integer port;
    private final Map<SocketAddress, Date> clones;

    public Receiver(String groupInetAddrName, Integer port, Map<SocketAddress, Date> clones) throws IOException {
        groupAddr = InetAddress.getByName(groupInetAddrName);
        this.port = port;
        this.clones = clones;

        multicastSocket = new MulticastSocket(port);
        multicastSocket.joinGroup(
                new InetSocketAddress(groupAddr, port),
                NetworkInterface.getByInetAddress(groupAddr)
        );
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            byte[] incMsg = new byte[1024];
            packet = new DatagramPacket(incMsg, incMsg.length, groupAddr, port);
            try {
                multicastSocket.receive(packet);
            } catch (IOException e) {
                System.err.println("Unable to receive msg: " + e.getLocalizedMessage());
                System.err.println("Shutdown.");
                Thread.currentThread().interrupt();
                break;
            }
            clones.put(packet.getSocketAddress(), new Date(System.currentTimeMillis()));
        }
        try {
            multicastSocket.leaveGroup(
                    new InetSocketAddress(groupAddr, port),
                    NetworkInterface.getByInetAddress(groupAddr)
            );
        } catch (IOException e) {
            System.err.println(Thread.currentThread().getName() + ": unable leave the group: " + e.getLocalizedMessage());
        }
        multicastSocket.close();
    }
}
