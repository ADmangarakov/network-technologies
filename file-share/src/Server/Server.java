package Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ServerSocket incomingClients;
    private final ExecutorService clientsHandlerService = Executors.newFixedThreadPool(4);
    private Server(int port, int backlog, InetAddress ip) throws IOException {
        incomingClients = new ServerSocket(port, backlog, ip);
    }
    public static void main(String[] args) {

    }
}
