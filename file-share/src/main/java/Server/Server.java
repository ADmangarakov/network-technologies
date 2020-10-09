package Server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private final ServerSocket incomingClients;
    private final ExecutorService clientsHandlerService = Executors.newFixedThreadPool(4);
    private final Thread viewThread;

    private Server(int port) throws IOException {
        incomingClients = new ServerSocket(port);
        viewThread = new Thread(View.getInstance());
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Enter the port!");
            return;
        }
        Server server;
        try {
            server = new Server(Integer.parseInt(args[0]));
        } catch (IOException e) {
            System.err.println("Unable to create server socket: " + e.getLocalizedMessage());
            return;
        }
        server.start();
    }

    private void start() {
        if (!Files.exists(Paths.get("./uploads"))) {
            if (!new File("./uploads").mkdir()) {
                System.err.println("Unable to create resource dir");
                return;
            }
        }

        viewThread.start();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                FileReceiver receiver = new FileReceiver(incomingClients.accept());
                clientsHandlerService.execute(receiver);
            } catch (IOException e) {
                System.err.println("Unable to accept incoming connection: " + e.getLocalizedMessage());
            }
        }
        shutdownAndAwaitTermination(clientsHandlerService);
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        try {
            viewThread.interrupt();
            incomingClients.close();
        } catch (IOException e) {
            System.err.println("Unable to close server socket!");
        }
    }
}
