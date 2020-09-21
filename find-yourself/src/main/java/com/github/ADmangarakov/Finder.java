package com.github.ADmangarakov;

import java.io.IOException;

public class Finder {
    private Sender sender;
    private Receiver receiver;
    private final View view;
    public Finder(String groupInetAddrName, Integer port) {
        try {
            sender = new Sender(groupInetAddrName, port);
        } catch (IOException e) {
            System.err.println("Unable to create sender: " + e.getLocalizedMessage());
            System.exit(1);
        }
        view = View.getInstance();
        try {
            receiver = new Receiver(groupInetAddrName, port, view.getClones());
        } catch (IOException e) {
            System.err.println("Unable to create receiver: " + e.getLocalizedMessage());
            System.exit(1);
        }

    }

    public static void main(String[] args){
        Finder finder = new Finder(args[0], Integer.parseInt(args[1]));
        finder.start();
    }

    public void start() {
        Thread sendThread = new Thread(sender, "Sender");
        Thread recvThread = new Thread(receiver, "Receiver");
        Thread viewThread = new Thread(view, "View");
        viewThread.start();
        recvThread.start();
        sendThread.start();
        while (sendThread.isAlive() && recvThread.isAlive() && viewThread.isAlive());
        viewThread.interrupt();
        recvThread.interrupt();
        sendThread.interrupt();
        try {
            viewThread.join();
            recvThread.join();
            sendThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
